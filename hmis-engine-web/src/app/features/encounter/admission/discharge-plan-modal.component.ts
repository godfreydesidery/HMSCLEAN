import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { AdmissionBillingSummary } from '../../billing/invoice.types';
import { ClosureDocumentComponent, ClosureDocumentData } from '../closure-document.component';
import { DischargePlanService } from './discharge-plan.service';
import {
  DISCHARGE_PLAN_KINDS, DischargePlan, DischargePlanKind, DischargePlanRequest
} from './discharge-plan.types';

/**
 * Author / approve a structured discharge plan (PROCESS_MISMATCHES.md M17).
 * The admission can only close once a plan of the matching kind is APPROVED —
 * and, by segregation of duties, the approver must differ from the author.
 */
@Component({
  selector: 'app-discharge-plan-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './discharge-plan-modal.component.html'
})
export class DischargePlanModalComponent implements OnInit {
  @Input({ required: true }) admissionUid!: string;
  /** Bill-clearance snapshot (V66 gate): when not cleared, closure is blocked. */
  @Input() billingSummary: AdmissionBillingSummary | null = null;
  /** Patient display fields for the printable closure document (DISCH-2). */
  @Input() patientName: string | null = null;
  @Input() patientNo: string | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(DischargePlanService);
  private readonly modal = inject(NgbModal);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly kinds = DISCHARGE_PLAN_KINDS;
  readonly plan = signal<DischargePlan | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    kind: ['DISCHARGE' as DischargePlanKind, [Validators.required]],
    history: ['', [Validators.maxLength(4000)]],
    investigation: ['', [Validators.maxLength(4000)]],
    management: ['', [Validators.maxLength(4000)]],
    operationNote: ['', [Validators.maxLength(4000)]],
    icuNote: ['', [Validators.maxLength(4000)]],
    recommendations: ['', [Validators.maxLength(4000)]],
    referralFacility: ['', [Validators.maxLength(200)]],
    referralReason: ['', [Validators.maxLength(1000)]],
    timeOfDeath: [''],
    causeOfDeath: ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    this.service.find(this.admissionUid).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (p) => { this.plan.set(p); this.patch(p); },
      error: () => { /* 404 — no plan yet, leave the blank create form */ }
    });
  }

  private patch(p: DischargePlan): void {
    this.form.patchValue({
      kind: p.kind,
      history: p.history ?? '', investigation: p.investigation ?? '', management: p.management ?? '',
      operationNote: p.operationNote ?? '', icuNote: p.icuNote ?? '', recommendations: p.recommendations ?? '',
      referralFacility: p.referralFacility ?? '', referralReason: p.referralReason ?? '',
      timeOfDeath: p.timeOfDeath ? p.timeOfDeath.substring(0, 16) : '', causeOfDeath: p.causeOfDeath ?? ''
    });
    if (p.status !== 'PENDING') this.form.disable();
  }

  get kind(): DischargePlanKind { return this.form.controls.kind.value; }
  get isNew(): boolean { return this.plan() === null; }
  get isPending(): boolean { return this.plan()?.status === 'PENDING'; }

  private payload(): DischargePlanRequest {
    const r = this.form.getRawValue();
    return {
      kind: this.isNew ? r.kind : undefined,
      history: r.history.trim() || null,
      investigation: r.investigation.trim() || null,
      management: r.management.trim() || null,
      operationNote: r.operationNote.trim() || null,
      icuNote: r.icuNote.trim() || null,
      recommendations: r.recommendations.trim() || null,
      referralFacility: r.referralFacility.trim() || null,
      referralReason: r.referralReason.trim() || null,
      timeOfDeath: r.timeOfDeath ? new Date(r.timeOfDeath).toISOString() : null,
      causeOfDeath: r.causeOfDeath.trim() || null
    };
  }

  save(): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.errorMessage.set(null);
    const obs = this.isNew
      ? this.service.create(this.admissionUid, this.payload())
      : this.service.update(this.admissionUid, this.payload());
    obs.pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (p) => { this.plan.set(p); this.patch(p); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save the plan.')
    });
  }

  approve(): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.errorMessage.set(null);
    this.service.approve(this.admissionUid).pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (p) => this.activeModal.close(p),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not approve the plan.')
    });
  }

  /** Open the printable closure document (DISCH-2) for the saved plan. */
  printDocument(): void {
    const p = this.plan();
    if (!p) return;
    const data: ClosureDocumentData = {
      kind: p.kind,
      status: p.status,
      patientName: this.patientName,
      patientNo: this.patientNo,
      encounterLabel: 'Admission',
      encounterNo: p.admissionNo,
      history: p.history,
      investigation: p.investigation,
      management: p.management,
      operationNote: p.operationNote,
      icuNote: p.icuNote,
      recommendations: p.recommendations,
      referralFacility: p.referralFacility,
      // Admission plans denormalise the provider name into referralFacility.
      externalProviderName: null,
      referralReason: p.referralReason,
      timeOfDeath: p.timeOfDeath,
      causeOfDeath: p.causeOfDeath,
      authoredByUsername: p.authoredByUsername,
      authoredAt: p.authoredAt,
      approvedByUsername: p.approvedByUsername,
      approvedAt: p.approvedAt
    };
    const ref = this.modal.open(ClosureDocumentComponent, { size: 'lg', scrollable: true });
    (ref.componentInstance as ClosureDocumentComponent).data = data;
  }
}
