import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Subject, debounceTime, distinctUntilChanged, finalize, startWith, switchMap } from 'rxjs';

import { ClosureDocumentComponent, ClosureDocumentData } from '../closure-document.component';
import { ExternalProviderService } from '../../masterdata/external-providers/external-provider.service';
import { ExternalMedicalProvider } from '../../masterdata/external-providers/external-provider.types';
import { ConsultationClosureService } from './consultation-closure.service';
import {
  ClosurePlan, CreateClosurePlanRequest, UpdateClosurePlanRequest
} from './consultation-closure.types';

/**
 * Author / approve an outpatient closure plan — outpatient death (DECEASED) or
 * external referral (REFERRAL). The consultation closes (status DECEASED / REFERRED)
 * only when a matching plan is APPROVED, and the approver must differ from the author.
 */
@Component({
  selector: 'app-consultation-closure-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './consultation-closure-modal.component.html'
})
export class ConsultationClosureModalComponent implements OnInit {
  @Input({ required: true }) consultationUid!: string;
  /** Mode the modal opens in when there is no existing plan yet. */
  @Input() initialKind: 'DECEASED' | 'REFERRAL' = 'DECEASED';
  /** Patient display fields for the printable closure document (DISCH-2). */
  @Input() patientName: string | null = null;
  @Input() patientNo: string | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ConsultationClosureService);
  private readonly providerService = inject(ExternalProviderService);
  private readonly modal = inject(NgbModal);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly plan = signal<ClosurePlan | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly providerSearch$ = new Subject<string>();
  readonly providers = signal<ExternalMedicalProvider[]>([]);

  readonly form = this.fb.nonNullable.group({
    history: ['', [Validators.required, Validators.maxLength(4000)]],
    investigation: ['', [Validators.maxLength(4000)]],
    management: ['', [Validators.required, Validators.maxLength(4000)]],
    operationNote: ['', [Validators.maxLength(4000)]],
    icuNote: ['', [Validators.maxLength(4000)]],
    recommendations: ['', [Validators.required, Validators.maxLength(4000)]],
    // REFERRAL
    externalProviderUid: ['', []],
    referralFacility: ['', [Validators.maxLength(200)]],
    referralReason: ['', [Validators.maxLength(1000)]],
    // DECEASED
    timeOfDeath: ['', []],
    causeOfDeath: ['', [Validators.maxLength(500)]]
  });

  /** The active kind: an existing plan's kind, else the requested initial kind. */
  private kindSig = signal<'DECEASED' | 'REFERRAL'>('DECEASED');

  ngOnInit(): void {
    this.kindSig.set(this.initialKind);
    this.providerSearch$.pipe(
      startWith(''),
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((q) => this.providerService.search({ query: q || undefined, active: true, size: 20, sort: 'name,asc' }))
    ).subscribe({
      next: (page) => this.providers.set(page.content),
      error: () => { /* picker stays empty; free-text facility is the fallback */ }
    });

    this.service.find(this.consultationUid).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (p) => { this.plan.set(p); this.kindSig.set(p.kind === 'REFERRAL' ? 'REFERRAL' : 'DECEASED'); this.patch(p); },
      error: () => { /* 404 — no plan yet, leave the blank create form in the requested kind */ }
    });
  }

  private patch(p: ClosurePlan): void {
    this.form.patchValue({
      history: p.history ?? '', investigation: p.investigation ?? '', management: p.management ?? '',
      operationNote: p.operationNote ?? '', icuNote: p.icuNote ?? '', recommendations: p.recommendations ?? '',
      externalProviderUid: p.externalProviderUid ?? '', referralFacility: p.referralFacility ?? '', referralReason: p.referralReason ?? '',
      timeOfDeath: p.timeOfDeath ? p.timeOfDeath.substring(0, 16) : '', causeOfDeath: p.causeOfDeath ?? ''
    });
    if (p.status !== 'PENDING') this.form.disable();
  }

  get kind(): 'DECEASED' | 'REFERRAL' { return this.kindSig(); }
  get isNew(): boolean { return this.plan() === null; }
  get isPending(): boolean { return this.plan()?.status === 'PENDING'; }
  get title(): string { return this.kind === 'DECEASED' ? 'Record death (outpatient)' : 'Refer out'; }

  onProviderSearch(value: string): void { this.providerSearch$.next(value); }

  private createPayload(): CreateClosurePlanRequest {
    return { kind: this.kind, ...this.sharedPayload() };
  }
  private updatePayload(): UpdateClosurePlanRequest {
    return this.sharedPayload();
  }
  private sharedPayload(): UpdateClosurePlanRequest {
    const r = this.form.getRawValue();
    return {
      history: r.history.trim() || null,
      investigation: r.investigation.trim() || null,
      management: r.management.trim() || null,
      operationNote: r.operationNote.trim() || null,
      icuNote: r.icuNote.trim() || null,
      recommendations: r.recommendations.trim() || null,
      externalProviderUid: r.externalProviderUid.trim() || null,
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
      ? this.service.create(this.consultationUid, this.createPayload())
      : this.service.update(this.consultationUid, this.updatePayload());
    obs.pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (p) => { this.plan.set(p); this.patch(p); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save the closure plan.')
    });
  }

  approve(): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.errorMessage.set(null);
    this.service.approve(this.consultationUid).pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (p) => this.activeModal.close(p),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not approve the closure plan.')
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
      encounterLabel: 'Consultation',
      encounterNo: p.consultationNo,
      history: p.history,
      investigation: p.investigation,
      management: p.management,
      operationNote: p.operationNote,
      icuNote: p.icuNote,
      recommendations: p.recommendations,
      referralFacility: p.referralFacility,
      externalProviderName: p.externalProviderName,
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
