import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { PharmacyService } from '../masterdata/pharmacies/pharmacy.service';
import { Pharmacy } from '../masterdata/pharmacies/pharmacy.types';
import { StoreService } from '../masterdata/stores/store.service';
import { Store } from '../masterdata/stores/store.types';
import { ConsumableIssueService, ConsumableMasterdataService } from './consumable.service';
import {
  CONSUMABLE_SOURCE_KINDS, Consumable, ConsumableIssue, ConsumableSourceKind
} from './consumable.types';

@Component({
  selector: 'app-issue-consumable-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './issue-consumable-modal.component.html'
})
export class IssueConsumableModalComponent implements OnInit {
  @Input({ required: true }) admissionUid!: string;

  private readonly fb = inject(FormBuilder);
  private readonly issueService = inject(ConsumableIssueService);
  private readonly consumableService = inject(ConsumableMasterdataService);
  private readonly pharmacyService = inject(PharmacyService);
  private readonly storeService = inject(StoreService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly sourceKinds = CONSUMABLE_SOURCE_KINDS;
  readonly pharmacies = signal<Pharmacy[]>([]);
  readonly stores = signal<Store[]>([]);
  readonly consumables = signal<Consumable[]>([]);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    consumableUid:     ['', [Validators.required]],
    sourceKind:        ['PHARMACY' as ConsumableSourceKind, [Validators.required]],
    sourceLocationUid: ['', [Validators.required]],
    quantity:          [1, [Validators.required, Validators.min(1)]],
    unitCost:          ['0.00', [Validators.required, Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
    note:              ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    this.pharmacyService.search({ active: true, size: 200, sort: 'name,asc' })
      .subscribe({ next: (r) => this.pharmacies.set(r.content), error: () => { /* */ } });
    this.storeService.search({ active: true, size: 200, sort: 'name,asc' })
      .subscribe({ next: (r) => this.stores.set(r.content), error: () => { /* */ } });
    this.consumableService.search({ active: true, size: 500, sort: 'name,asc' })
      .subscribe({ next: (r) => this.consumables.set(r.content), error: () => { /* */ } });
  }

  get locations(): Array<{ uid: string; label: string }> {
    return this.form.controls.sourceKind.value === 'PHARMACY'
      ? this.pharmacies().map((p) => ({ uid: p.uid, label: `${p.code} — ${p.name}` }))
      : this.stores().map((s) => ({ uid: s.uid, label: `${s.code} — ${s.name}` }));
  }

  onSourceKindChange(): void {
    // Reset the location so we don't keep a pharmacy uid selected after switching to STORE.
    this.form.controls.sourceLocationUid.setValue('');
  }

  submit(): void {
    if (this.submitting()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.issueService.issue(this.admissionUid, {
      consumableUid:     raw.consumableUid,
      sourceKind:        raw.sourceKind,
      sourceLocationUid: raw.sourceLocationUid,
      quantity:          Number(raw.quantity),
      unitCost:          raw.unitCost,
      note:              raw.note?.trim() || null
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (saved: ConsumableIssue) => this.activeModal.close(saved),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not record issue.')
    });
  }
}
