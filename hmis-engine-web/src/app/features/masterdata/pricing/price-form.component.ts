import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, finalize } from 'rxjs';

import { PageResponse } from '../../../core/http/page.types';
import { ClinicService } from '../clinics/clinic.service';
import { CurrencyService } from '../currencies/currency.service';
import { Currency } from '../currencies/currency.types';
import { InsurancePlanService } from '../insurance-plans/insurance-plan.service';
import { InsurancePlan } from '../insurance-plans/insurance-plan.types';
import { LabTestTypeService } from '../lab-tests/lab-test.service';
import { MedicineService } from '../medicines/medicine.service';
import { ProcedureTypeService } from '../procedures/procedure.service';
import { RadiologyTypeService } from '../radiology/radiology.service';
import { WardService } from '../wards/ward.service';
import { ServicePriceService } from './service-price.service';
import { SERVICE_KINDS, ServiceKind, ServicePrice } from './service-price.types';

interface ServiceOption {
  uid: string;
  label: string;
}

@Component({
  selector: 'app-price-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './price-form.component.html'
})
export class PriceFormComponent implements OnInit {
  @Input() existing: ServicePrice | null = null;

  /** Optional pre-scoping for a "set price for this service" shortcut (e.g. a clinic's consultation fee). */
  @Input() presetKind: ServiceKind | null = null;
  @Input() presetServiceUid: string | null = null;
  @Input() presetServiceLabel: string | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly priceService = inject(ServicePriceService);
  private readonly planService = inject(InsurancePlanService);
  private readonly currencyService = inject(CurrencyService);

  private readonly clinicService = inject(ClinicService);
  private readonly labService = inject(LabTestTypeService);
  private readonly procedureService = inject(ProcedureTypeService);
  private readonly radiologyService = inject(RadiologyTypeService);
  private readonly medicineService = inject(MedicineService);
  private readonly wardService = inject(WardService);

  protected readonly activeModal = inject(NgbActiveModal);

  readonly serviceKinds = SERVICE_KINDS;
  readonly plans = signal<InsurancePlan[]>([]);
  readonly currencies = signal<Currency[]>([]);
  readonly serviceOptions = signal<ServiceOption[]>([]);
  readonly servicesLoading = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    planUid: [''],                          // empty = cash price
    kind: ['CONSULTATION' as ServiceKind, [Validators.required]],
    serviceUid: ['', [Validators.required]],
    amount: [0, [Validators.required, Validators.min(0)]],
    minAmount: [null as number | null, [Validators.min(0)]],
    maxAmount: [null as number | null, [Validators.min(0)]],
    currency: ['TZS', [Validators.required, Validators.pattern(/^[A-Z]{3}$/)]],
    note: ['', [Validators.maxLength(255)]]
  });

  ngOnInit(): void {
    this.planService.search({ active: true, size: 200, sort: 'name,asc' }).subscribe({
      next: (res) => this.plans.set(res.content),
      error: () => { /* ignore */ }
    });
    this.currencyService.search({ active: true, size: 200, sort: 'code,asc' }).subscribe({
      next: (res) => {
        this.currencies.set(res.content);
        // Default a brand-new entry to the system default currency.
        if (!this.existing) {
          const def = res.content.find((c) => c.isDefault) ?? res.content[0];
          if (def) this.form.controls.currency.setValue(def.code);
        }
      },
      error: () => { /* ignore */ }
    });

    if (this.existing) {
      const e = this.existing;
      this.form.patchValue({
        planUid: e.planUid ?? '',
        kind: e.kind,
        serviceUid: e.serviceUid,
        amount: e.amount,
        minAmount: e.minAmount,
        maxAmount: e.maxAmount,
        currency: e.currency,
        note: e.note ?? ''
      });
      this.form.controls.kind.disable();
      this.form.controls.serviceUid.disable();
      this.form.controls.planUid.disable();
      this.form.controls.currency.disable();   // currency is part of the price key — edit amount/band, not currency
      this.loadServiceOptions(e.kind, e.serviceUid, e.serviceName ?? '');
    } else if (this.presetServiceUid && this.presetKind) {
      // Pre-scoped to a specific service (e.g. a clinic): lock kind + service, allow plan/amount/band.
      this.form.patchValue({ kind: this.presetKind, serviceUid: this.presetServiceUid });
      this.form.controls.kind.disable();
      this.form.controls.serviceUid.disable();
      this.loadServiceOptions(this.presetKind, this.presetServiceUid, this.presetServiceLabel ?? '');
    } else {
      this.loadServiceOptions(this.form.controls.kind.value);
      this.form.controls.kind.valueChanges.subscribe((k) => {
        this.form.controls.serviceUid.setValue('');
        this.loadServiceOptions(k);
      });
    }
  }

  get isEdit(): boolean { return !!this.existing; }
  get title(): string {
    if (this.isEdit) return 'Edit price';
    if (this.presetServiceLabel) return `Set price — ${this.presetServiceLabel}`;
    return 'Set price';
  }

  private loadServiceOptions(kind: ServiceKind, ensureUid?: string, ensureLabel?: string): void {
    this.servicesLoading.set(true);
    const obs = this.optionsStreamFor(kind);
    obs.pipe(finalize(() => this.servicesLoading.set(false))).subscribe({
      next: (res) => {
        const options = res.content;
        if (ensureUid && !options.find((o) => o.uid === ensureUid)) {
          options.unshift({ uid: ensureUid, label: ensureLabel || ensureUid });
        }
        this.serviceOptions.set(options);
      },
      error: () => this.serviceOptions.set([])
    });
  }

  private optionsStreamFor(kind: ServiceKind): Observable<PageResponse<ServiceOption>> {
    switch (kind) {
      case 'CONSULTATION':
        return this.clinicService.search({ active: true, size: 200, sort: 'name,asc' }).pipe(
          mapToOptions((c) => ({ uid: c.uid, label: `${c.name} (${c.code})` })));
      case 'LAB_TEST':
        return this.labService.search({ active: true, size: 200, sort: 'name,asc' }).pipe(
          mapToOptions((l) => ({ uid: l.uid, label: `${l.name} (${l.code})` })));
      case 'PROCEDURE':
        return this.procedureService.search({ active: true, size: 200, sort: 'name,asc' }).pipe(
          mapToOptions((p) => ({ uid: p.uid, label: `${p.name} (${p.code})` })));
      case 'RADIOLOGY':
        return this.radiologyService.search({ active: true, size: 200, sort: 'name,asc' }).pipe(
          mapToOptions((r) => ({ uid: r.uid, label: `${r.name} (${r.code})` })));
      case 'MEDICINE':
        return this.medicineService.search({ active: true, size: 200, sort: 'name,asc' }).pipe(
          mapToOptions((m) => ({ uid: m.uid, label: `${m.name}${m.strength ? ' ' + m.strength : ''}` })));
      case 'WARD':
        return this.wardService.search({ active: true, size: 200, sort: 'name,asc' }).pipe(
          mapToOptions((w) => ({ uid: w.uid, label: `${w.name} (${w.code})` })));
    }
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const min = raw.minAmount === null || raw.minAmount === undefined ? null : Number(raw.minAmount);
    const max = raw.maxAmount === null || raw.maxAmount === undefined ? null : Number(raw.maxAmount);
    const existing = this.existing;
    const req$ = existing
      ? this.priceService.update(existing.uid, {
          amount: Number(raw.amount), minAmount: min, maxAmount: max, note: raw.note?.trim() || null
        })
      : this.priceService.create({
          planUid: raw.planUid ? raw.planUid : null,
          kind: raw.kind,
          serviceUid: raw.serviceUid,
          amount: Number(raw.amount),
          minAmount: min,
          maxAmount: max,
          currency: raw.currency.toUpperCase(),
          note: raw.note?.trim() || null
        });
    req$.pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (p) => this.activeModal.close(p),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save price.')
      });
  }
}

import { map } from 'rxjs/operators';
function mapToOptions<T extends { uid: string }>(toOption: (t: T) => ServiceOption) {
  return map<PageResponse<T>, PageResponse<ServiceOption>>((res) => ({
    ...res,
    content: res.content.map(toOption)
  }));
}
