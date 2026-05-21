import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgbDropdownModule, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize, forkJoin } from 'rxjs';

import { RecordPaymentComponent } from '../../billing/record-payment.component';
import { InvoiceService } from '../../billing/invoice.service';
import { INVOICE_STATUSES, Invoice, InvoiceStatus } from '../../billing/invoice.types';
import { ConsumableIssueService } from '../../consumables/consumable.service';
import { ConsumableIssue } from '../../consumables/consumable.types';
import { IssueConsumableModalComponent } from '../../consumables/issue-consumable-modal.component';
import { WardService } from '../../masterdata/wards/ward.service';
import { Ward } from '../../masterdata/wards/ward.types';
import { PAYMENT_TYPES, PaymentType } from '../../patient/patient.types';
import { AdmissionService } from './admission.service';
import { ADMISSION_STATUSES, Admission, AdmissionStatus } from './admission.types';
import { DischargePlanModalComponent } from './discharge-plan-modal.component';
import { DischargePlan } from './discharge-plan.types';
import { ProgressNoteService } from './progress-note.service';
import { PROGRESS_NOTE_KINDS, ProgressNote, ProgressNoteKind } from './progress-note.types';

type TabKey = 'overview' | 'notes' | 'consumables' | 'billing';

@Component({
  selector: 'app-admission-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, NgbDropdownModule],
  templateUrl: './admission-detail.component.html'
})
export class AdmissionDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly admissionService = inject(AdmissionService);
  private readonly wardService = inject(WardService);
  private readonly noteService = inject(ProgressNoteService);
  private readonly invoiceService = inject(InvoiceService);
  private readonly consumableIssueService = inject(ConsumableIssueService);
  private readonly modal = inject(NgbModal);
  private readonly fb = inject(FormBuilder);

  readonly statuses = ADMISSION_STATUSES;
  readonly paymentTypes = PAYMENT_TYPES;
  readonly noteKinds = PROGRESS_NOTE_KINDS;
  readonly invoiceStatuses = INVOICE_STATUSES;

  readonly admission = signal<Admission | null>(null);
  readonly wards = signal<Ward[]>([]);
  readonly notes = signal<ProgressNote[]>([]);
  readonly invoice = signal<Invoice | null>(null);
  readonly consumables = signal<ConsumableIssue[]>([]);

  readonly loading = signal(true);
  readonly notesLoading = signal(false);
  readonly invoiceLoading = signal(false);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly actionMessage = signal<string | null>(null);

  readonly activeTab = signal<TabKey>('overview');

  readonly isActive = computed(() => this.admission()?.status === 'ADMITTED');

  readonly transferForm = this.fb.nonNullable.group({
    wardUid: ['', [Validators.required]],
    bedLabel: ['', [Validators.maxLength(32)]]
  });
  readonly cancelForm = this.fb.nonNullable.group({
    reason: ['', [Validators.maxLength(255)]]
  });
  readonly noteForm = this.fb.nonNullable.group({
    kind: ['DOCTOR' as ProgressNoteKind, [Validators.required]],
    body: ['', [Validators.required, Validators.maxLength(8000)]]
  });

  constructor() {
    const uid = this.route.snapshot.paramMap.get('uid');
    if (!uid) {
      this.errorMessage.set('Missing admission identifier.');
      this.loading.set(false);
      return;
    }
    this.load(uid);
    this.wardService.search({ active: true, size: 200, sort: 'name,asc' }).subscribe({
      next: (page) => this.wards.set(page.content)
    });
  }

  private load(uid: string): void {
    this.loading.set(true);
    this.notesLoading.set(true);
    this.invoiceLoading.set(true);
    forkJoin({
      admission: this.admissionService.findByUid(uid),
      notes: this.noteService.list(uid),
      invoice: this.invoiceService.findForAdmission(uid),
      consumables: this.consumableIssueService.listForAdmission(uid)
    }).pipe(finalize(() => {
      this.loading.set(false);
      this.notesLoading.set(false);
      this.invoiceLoading.set(false);
    })).subscribe({
      next: ({ admission, notes, invoice, consumables }) => {
        this.admission.set(admission);
        this.notes.set(notes);
        this.invoice.set(invoice);
        this.consumables.set(consumables);
      },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load admission.')
    });
  }

  setTab(tab: TabKey): void { this.activeTab.set(tab); }

  openIssueConsumable(): void {
    const a = this.admission();
    if (!a) return;
    const ref = this.modal.open(IssueConsumableModalComponent, { size: 'lg', backdrop: 'static' });
    const inst = ref.componentInstance as IssueConsumableModalComponent;
    inst.admissionUid = a.uid;
    ref.closed.subscribe((saved?: ConsumableIssue) => {
      if (saved) this.consumables.update((rows) => [...rows, saved]);
    });
  }

  // ----- ward transfer / discharge / cancel --------------------------------

  openTransfer(content: unknown): void {
    const a = this.admission();
    if (!a) return;
    this.transferForm.reset({ wardUid: a.wardUid, bedLabel: a.bedLabel ?? '' });
    this.modal.open(content, { centered: true });
  }

  confirmTransfer(modalRef: { dismiss: () => void }): void {
    const a = this.admission(); if (!a) return;
    if (this.transferForm.invalid) { this.transferForm.markAllAsTouched(); return; }
    const raw = this.transferForm.getRawValue();
    this.busy.set(true);
    this.admissionService.transferWard(a.uid, { wardUid: raw.wardUid, bedLabel: raw.bedLabel?.trim() || null })
      .pipe(finalize(() => this.busy.set(false))).subscribe({
        next: (updated) => { this.admission.set(updated); this.actionMessage.set('Ward updated.'); modalRef.dismiss(); },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Transfer failed.')
      });
  }

  /**
   * Discharge / deceased / referral all go through the structured discharge
   * plan (PROCESS_MISMATCHES.md M17): author the plan, then a different user
   * approves it — approval is what closes the admission. The direct
   * discharge endpoints are gated server-side on an APPROVED plan.
   */
  openDischargePlan(): void {
    const a = this.admission(); if (!a) return;
    const ref = this.modal.open(DischargePlanModalComponent, { size: 'lg', backdrop: 'static', scrollable: true });
    (ref.componentInstance as DischargePlanModalComponent).admissionUid = a.uid;
    ref.closed.subscribe((plan: DischargePlan | undefined) => {
      if (!plan) return;
      // Approval closed the admission — refresh the view.
      this.actionMessage.set('Discharge plan approved — admission closed.');
      this.admissionService.findByUid(a.uid).subscribe({
        next: (updated) => this.admission.set(updated),
        error: () => { /* keep previous */ }
      });
    });
  }

  openCancel(content: unknown): void {
    this.cancelForm.reset({ reason: '' });
    this.modal.open(content, { centered: true }).result.then((action) => {
      if (action === 'cancel') this.confirmCancel();
    }, () => {});
  }

  private confirmCancel(): void {
    const a = this.admission(); if (!a) return;
    const reason = this.cancelForm.controls.reason.value.trim() || null;
    this.busy.set(true);
    this.admissionService.cancel(a.uid, reason).pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (updated) => { this.admission.set(updated); this.actionMessage.set('Admission cancelled.'); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Cancel failed.')
    });
  }

  // ----- progress notes ----------------------------------------------------

  addNote(): void {
    const a = this.admission(); if (!a) return;
    if (this.noteForm.invalid) { this.noteForm.markAllAsTouched(); return; }
    const raw = this.noteForm.getRawValue();
    this.busy.set(true);
    this.noteService.add(a.uid, { kind: raw.kind, body: raw.body.trim() })
      .pipe(finalize(() => this.busy.set(false))).subscribe({
        next: (note) => {
          this.notes.update((arr) => [note, ...arr]);
          this.noteForm.reset({ kind: raw.kind, body: '' });
          this.actionMessage.set('Progress note recorded.');
        },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save note.')
      });
  }

  deleteNote(note: ProgressNote): void {
    const reason = globalThis.prompt('Reason for removing this note?')?.trim() ?? null;
    if (reason === null) return;
    this.noteService.softDelete(note.uid, reason || null).subscribe({
      next: (updated) => this.notes.update((arr) => arr.map((n) => n.uid === updated.uid ? updated : n)),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not remove note.')
    });
  }

  // ----- billing -----------------------------------------------------------

  generateInvoice(): void {
    const a = this.admission(); if (!a) return;
    this.invoiceLoading.set(true);
    this.invoiceService.generateForAdmission(a.uid)
      .pipe(finalize(() => this.invoiceLoading.set(false))).subscribe({
        next: (inv) => { this.invoice.set(inv); this.actionMessage.set('Invoice generated.'); },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not generate invoice.')
      });
  }

  issueInvoice(): void {
    const inv = this.invoice(); if (!inv) return;
    this.invoiceService.issue(inv.uid).subscribe({
      next: (updated) => { this.invoice.set(updated); this.actionMessage.set('Invoice issued.'); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not issue invoice.')
    });
  }

  cancelInvoice(): void {
    const inv = this.invoice(); if (!inv) return;
    const reason = globalThis.prompt('Reason for cancelling this invoice?')?.trim() ?? null;
    this.invoiceService.cancel(inv.uid, reason).subscribe({
      next: (updated) => { this.invoice.set(updated); this.actionMessage.set('Invoice cancelled.'); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not cancel invoice.')
    });
  }

  recordInvoicePayment(): void {
    const inv = this.invoice(); if (!inv) return;
    const ref = this.modal.open(RecordPaymentComponent, { backdrop: 'static' });
    const inst = ref.componentInstance as RecordPaymentComponent;
    inst.invoice = inv;
    ref.closed.subscribe((updated: Invoice | undefined) => {
      if (updated) {
        this.invoice.set(updated);
        this.actionMessage.set('Payment recorded.');
      }
    });
  }

  backToList(): void { void this.router.navigate(['/encounters', 'admissions']); }

  // ----- helpers -----------------------------------------------------------

  statusBadgeClass(s: AdmissionStatus): string {
    return 'badge ' + (this.statuses.find((x) => x.value === s)?.badgeClass ?? '');
  }
  statusLabel(s: AdmissionStatus): string {
    return this.statuses.find((x) => x.value === s)?.label ?? s;
  }
  paymentLabel(p: PaymentType): string {
    return this.paymentTypes.find((x) => x.value === p)?.label ?? p;
  }
  noteKindBadgeClass(k: ProgressNoteKind): string {
    return 'badge d-inline-flex align-items-center gap-1 ' + (this.noteKinds.find((x) => x.value === k)?.badgeClass ?? '');
  }
  noteKindIcon(k: ProgressNoteKind): string {
    return this.noteKinds.find((x) => x.value === k)?.icon ?? 'bi-journal-text';
  }
  noteKindLabel(k: ProgressNoteKind): string {
    return this.noteKinds.find((x) => x.value === k)?.label ?? k;
  }
  invoiceStatusBadgeClass(s: InvoiceStatus): string {
    return 'badge ' + (this.invoiceStatuses.find((x) => x.value === s)?.badgeClass ?? '');
  }
  invoiceStatusLabel(s: InvoiceStatus): string {
    return this.invoiceStatuses.find((x) => x.value === s)?.label ?? s;
  }
  patientInitials(a: Admission): string {
    const parts = (a.patientName ?? '').split(' ').filter((p) => p.length > 0);
    if (parts.length === 0) return '?';
    const first = parts[0][0] ?? '';
    const last = parts.length > 1 ? parts[parts.length - 1][0] : '';
    return (first + last).toUpperCase();
  }
}
