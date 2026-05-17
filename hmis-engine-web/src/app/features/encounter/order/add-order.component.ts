import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, finalize } from 'rxjs';
import { map } from 'rxjs/operators';

import { PageResponse } from '../../../core/http/page.types';
import { LabTestTypeService } from '../../masterdata/lab-tests/lab-test.service';
import { ProcedureTypeService } from '../../masterdata/procedures/procedure.service';
import { RadiologyTypeService } from '../../masterdata/radiology/radiology.service';
import { ClinicalOrderService } from './clinical-order.service';
import {
  CLINICAL_ORDER_KINDS, ClinicalOrderKind, ORDER_URGENCIES, OrderUrgency
} from './clinical-order.types';

interface ServiceOption {
  uid: string;
  label: string;
}

@Component({
  selector: 'app-add-order',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './add-order.component.html'
})
export class AddOrderComponent implements OnInit {
  @Input({ required: true }) consultationUid!: string;
  @Input() initialKind: ClinicalOrderKind = 'LAB_TEST';

  private readonly fb = inject(FormBuilder);
  private readonly orderService = inject(ClinicalOrderService);
  private readonly labService = inject(LabTestTypeService);
  private readonly radiologyService = inject(RadiologyTypeService);
  private readonly procedureService = inject(ProcedureTypeService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly kinds = CLINICAL_ORDER_KINDS;
  readonly urgencies = ORDER_URGENCIES;
  readonly options = signal<ServiceOption[]>([]);
  readonly loadingOptions = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    kind: ['LAB_TEST' as ClinicalOrderKind, [Validators.required]],
    serviceUid: ['', [Validators.required]],
    urgency: ['NORMAL' as OrderUrgency, [Validators.required]],
    instructions: ['', [Validators.maxLength(1000)]]
  });

  ngOnInit(): void {
    this.form.controls.kind.setValue(this.initialKind);
    this.loadOptions(this.initialKind);
    this.form.controls.kind.valueChanges.subscribe((k) => {
      this.form.controls.serviceUid.setValue('');
      this.loadOptions(k);
    });
  }

  private loadOptions(kind: ClinicalOrderKind): void {
    this.loadingOptions.set(true);
    this.optionsFor(kind)
      .pipe(finalize(() => this.loadingOptions.set(false)))
      .subscribe({
        next: (res) => this.options.set(res.content),
        error: () => this.options.set([])
      });
  }

  private optionsFor(kind: ClinicalOrderKind): Observable<PageResponse<ServiceOption>> {
    switch (kind) {
      case 'LAB_TEST':
        return this.labService.search({ active: true, size: 300, sort: 'name,asc' }).pipe(
          map((res) => ({ ...res, content: res.content.map((l) => ({ uid: l.uid, label: `${l.code} — ${l.name}` })) })));
      case 'RADIOLOGY':
        return this.radiologyService.search({ active: true, size: 300, sort: 'name,asc' }).pipe(
          map((res) => ({ ...res, content: res.content.map((r) => ({ uid: r.uid, label: `${r.code} — ${r.name}` })) })));
      case 'PROCEDURE':
        return this.procedureService.search({ active: true, size: 300, sort: 'name,asc' }).pipe(
          map((res) => ({ ...res, content: res.content.map((p) => ({ uid: p.uid, label: `${p.code} — ${p.name}` })) })));
    }
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.orderService.request(this.consultationUid, {
      kind: raw.kind,
      serviceUid: raw.serviceUid,
      urgency: raw.urgency,
      instructions: raw.instructions?.trim() || null
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (o) => this.activeModal.close(o),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not raise order.')
    });
  }
}
