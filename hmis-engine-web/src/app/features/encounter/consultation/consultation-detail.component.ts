import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgbDropdownModule, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize, forkJoin } from 'rxjs';

import { InvoiceService } from '../../billing/invoice.service';
import { INVOICE_STATUSES, Invoice, InvoiceStatus } from '../../billing/invoice.types';
import { RecordPaymentComponent } from '../../billing/record-payment.component';
import { AddDiagnosisComponent } from '../diagnosis/add-diagnosis.component';
import { ConsultationDiagnosisService } from '../diagnosis/consultation-diagnosis.service';
import {
  ConsultationDiagnosis, DIAGNOSIS_KINDS, DiagnosisKind
} from '../diagnosis/consultation-diagnosis.types';
import { ClinicalNoteService } from '../note/clinical-note.service';
import { ClinicalNote } from '../note/clinical-note.types';
import { AddOrderComponent } from '../order/add-order.component';
import { ClinicalOrderService } from '../order/clinical-order.service';
import {
  CLINICAL_ORDER_KINDS, CLINICAL_ORDER_STATUSES, ClinicalOrder, ClinicalOrderKind, ClinicalOrderStatus,
  ORDER_URGENCIES, OrderUrgency
} from '../order/clinical-order.types';
import { AddPrescriptionComponent } from '../prescription/add-prescription.component';
import { PrescriptionService } from '../prescription/prescription.service';
import {
  PRESCRIPTION_STATUSES, Prescription, PrescriptionStatus
} from '../prescription/prescription.types';
import { VitalsFormComponent } from '../vitals/vitals-form.component';
import { VitalsService } from '../vitals/vitals.service';
import { PatientVitals } from '../vitals/vitals.types';
import { ConsultationService } from './consultation.service';
import { CONSULTATION_STATUSES, Consultation, ConsultationStatus } from './consultation.types';

type TabKey = 'overview' | 'vitals' | 'notes' | 'diagnoses' | 'orders' | 'billing';

@Component({
  selector: 'app-consultation-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, NgbDropdownModule],
  templateUrl: './consultation-detail.component.html',
  styleUrl: './consultation-detail.component.scss'
})
export class ConsultationDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly consultationService = inject(ConsultationService);
  private readonly vitalsService = inject(VitalsService);
  private readonly noteService = inject(ClinicalNoteService);
  private readonly diagnosisService = inject(ConsultationDiagnosisService);
  private readonly orderService = inject(ClinicalOrderService);
  private readonly prescriptionService = inject(PrescriptionService);
  private readonly invoiceService = inject(InvoiceService);
  private readonly modal = inject(NgbModal);
  private readonly fb = inject(FormBuilder);

  readonly statuses = CONSULTATION_STATUSES;
  readonly diagnosisKinds = DIAGNOSIS_KINDS;
  readonly orderKinds = CLINICAL_ORDER_KINDS;
  readonly orderStatuses = CLINICAL_ORDER_STATUSES;
  readonly orderUrgencies = ORDER_URGENCIES;
  readonly prescriptionStatuses = PRESCRIPTION_STATUSES;
  readonly invoiceStatuses = INVOICE_STATUSES;
  readonly consultation = signal<Consultation | null>(null);
  readonly vitals = signal<PatientVitals[]>([]);
  readonly diagnoses = signal<ConsultationDiagnosis[]>([]);
  readonly orders = signal<ClinicalOrder[]>([]);
  readonly prescriptions = signal<Prescription[]>([]);
  readonly invoice = signal<Invoice | null>(null);
  readonly invoiceLoading = signal(false);
  readonly note = signal<ClinicalNote | null>(null);
  readonly noteDirty = signal(false);
  readonly notesSaving = signal(false);
  readonly notesSavedAt = signal<string | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly activeTab = signal<TabKey>('overview');

  readonly noteForm = this.fb.nonNullable.group({
    chiefComplaint: [''],
    historyOfPresentingIllness: [''],
    pastMedicalHistory: [''],
    examination: [''],
    assessment: [''],
    plan: ['']
  });

  readonly canStart = computed(() => this.consultation()?.status === 'BOOKED');
  readonly canComplete = computed(() => this.consultation()?.status === 'IN_PROGRESS');
  readonly canCancel = computed(() => {
    const s = this.consultation()?.status;
    return s === 'BOOKED' || s === 'IN_PROGRESS';
  });
  readonly isEditable = computed(() => {
    const s = this.consultation()?.status;
    return s === 'BOOKED' || s === 'IN_PROGRESS';
  });

  readonly workingDiagnoses = computed(() => this.diagnoses().filter((d) => d.kind === 'WORKING'));
  readonly finalDiagnoses = computed(() => this.diagnoses().filter((d) => d.kind === 'FINAL'));

  constructor() {
    const uid = this.route.snapshot.paramMap.get('uid');
    if (!uid) {
      this.loading.set(false);
      this.errorMessage.set('Missing consultation identifier.');
      return;
    }
    forkJoin({
      consultation: this.consultationService.findByUid(uid),
      vitals: this.vitalsService.list(uid),
      note: this.noteService.get(uid),
      diagnoses: this.diagnosisService.list(uid),
      orders: this.orderService.list(uid),
      prescriptions: this.prescriptionService.list(uid),
      invoice: this.invoiceService.findForConsultation(uid)
    }).subscribe({
      next: ({ consultation, vitals, note, diagnoses, orders, prescriptions, invoice }) => {
        this.consultation.set(consultation);
        this.vitals.set(vitals);
        this.diagnoses.set(diagnoses);
        this.orders.set(orders);
        this.prescriptions.set(prescriptions);
        this.invoice.set(invoice);
        this.setNote(note);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.message ?? 'Could not load consultation.');
        this.loading.set(false);
      }
    });

    this.noteForm.valueChanges.subscribe(() => this.noteDirty.set(true));
  }

  setTab(tab: TabKey): void { this.activeTab.set(tab); }
  back(): void { void this.router.navigate(['/encounters', 'consultations']); }

  start(): void {
    const c = this.consultation();
    if (!c) return;
    this.consultationService.start(c.uid).subscribe({
      next: (updated) => this.consultation.set(updated),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not start consultation.')
    });
  }

  complete(): void {
    const c = this.consultation();
    if (!c) return;
    if (!globalThis.confirm('Mark this consultation as completed?')) return;
    this.consultationService.complete(c.uid).subscribe({
      next: (updated) => this.consultation.set(updated),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not complete consultation.')
    });
  }

  cancel(): void {
    const c = this.consultation();
    if (!c) return;
    const reason = globalThis.prompt('Reason for cancelling this consultation?')?.trim() ?? null;
    this.consultationService.cancel(c.uid, reason).subscribe({
      next: (updated) => this.consultation.set(updated),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not cancel consultation.')
    });
  }

  recordVitals(): void {
    const c = this.consultation();
    if (!c) return;
    const ref = this.modal.open(VitalsFormComponent, { size: 'lg', backdrop: 'static' });
    (ref.componentInstance as VitalsFormComponent).consultationUid = c.uid;
    ref.closed.subscribe(() => this.refreshVitals());
  }

  deleteVitals(v: PatientVitals): void {
    if (!globalThis.confirm('Delete this vitals reading?')) return;
    const c = this.consultation();
    if (!c) return;
    this.vitalsService.delete(c.uid, v.uid).subscribe({
      next: () => this.refreshVitals(),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not delete vitals.')
    });
  }

  saveNote(): void {
    const c = this.consultation();
    if (!c || this.notesSaving()) return;
    this.notesSaving.set(true);
    this.errorMessage.set(null);
    const raw = this.noteForm.getRawValue();
    const payload = {
      chiefComplaint: emptyToNull(raw.chiefComplaint),
      historyOfPresentingIllness: emptyToNull(raw.historyOfPresentingIllness),
      pastMedicalHistory: emptyToNull(raw.pastMedicalHistory),
      examination: emptyToNull(raw.examination),
      assessment: emptyToNull(raw.assessment),
      plan: emptyToNull(raw.plan)
    };
    this.noteService.save(c.uid, payload)
      .pipe(finalize(() => this.notesSaving.set(false))).subscribe({
        next: (saved) => {
          this.setNote(saved);
          this.notesSavedAt.set(new Date().toLocaleTimeString());
        },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save clinical note.')
      });
  }

  addDiagnosis(initialKind: DiagnosisKind): void {
    const c = this.consultation();
    if (!c) return;
    const ref = this.modal.open(AddDiagnosisComponent, { size: 'lg', backdrop: 'static' });
    const inst = ref.componentInstance as AddDiagnosisComponent;
    inst.consultationUid = c.uid;
    inst.initialKind = initialKind;
    ref.closed.subscribe(() => this.refreshDiagnoses());
  }

  removeDiagnosis(d: ConsultationDiagnosis): void {
    if (!globalThis.confirm(`Remove diagnosis "${d.diagnosisName ?? d.diagnosisTypeUid}"?`)) return;
    const c = this.consultation();
    if (!c) return;
    this.diagnosisService.remove(c.uid, d.uid).subscribe({
      next: () => this.refreshDiagnoses(),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not remove diagnosis.')
    });
  }

  private refreshVitals(): void {
    const c = this.consultation();
    if (!c) return;
    this.vitalsService.list(c.uid).subscribe({
      next: (vs) => this.vitals.set(vs),
      error: () => { /* keep existing */ }
    });
  }

  private refreshDiagnoses(): void {
    const c = this.consultation();
    if (!c) return;
    this.diagnosisService.list(c.uid).subscribe({
      next: (ds) => this.diagnoses.set(ds),
      error: () => { /* keep existing */ }
    });
  }

  // ----- Orders ----------------------------------------------------------

  addOrder(initialKind: ClinicalOrderKind): void {
    const c = this.consultation();
    if (!c) return;
    const ref = this.modal.open(AddOrderComponent, { size: 'lg', backdrop: 'static' });
    const inst = ref.componentInstance as AddOrderComponent;
    inst.consultationUid = c.uid;
    inst.initialKind = initialKind;
    ref.closed.subscribe(() => this.refreshOrders());
  }

  startOrder(o: ClinicalOrder): void {
    this.orderService.start(o.uid).subscribe({
      next: () => this.refreshOrders(),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not start order.')
    });
  }

  completeOrder(o: ClinicalOrder): void {
    const result = globalThis.prompt('Result / report for this order (optional):')?.trim() ?? null;
    this.orderService.complete(o.uid, result).subscribe({
      next: () => this.refreshOrders(),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not complete order.')
    });
  }

  cancelOrder(o: ClinicalOrder): void {
    const reason = globalThis.prompt('Reason for cancelling this order?')?.trim() ?? null;
    this.orderService.cancel(o.uid, reason).subscribe({
      next: () => this.refreshOrders(),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not cancel order.')
    });
  }

  private refreshOrders(): void {
    const c = this.consultation();
    if (!c) return;
    this.orderService.list(c.uid).subscribe({
      next: (os) => this.orders.set(os),
      error: () => { /* keep existing */ }
    });
  }

  // ----- Prescriptions ---------------------------------------------------

  addPrescription(): void {
    const c = this.consultation();
    if (!c) return;
    const ref = this.modal.open(AddPrescriptionComponent, { size: 'lg', backdrop: 'static' });
    (ref.componentInstance as AddPrescriptionComponent).consultationUid = c.uid;
    ref.closed.subscribe(() => this.refreshPrescriptions());
  }

  cancelPrescription(p: Prescription): void {
    const reason = globalThis.prompt('Reason for cancelling this prescription?')?.trim() ?? null;
    this.prescriptionService.cancel(p.uid, reason).subscribe({
      next: () => this.refreshPrescriptions(),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not cancel prescription.')
    });
  }

  private refreshPrescriptions(): void {
    const c = this.consultation();
    if (!c) return;
    this.prescriptionService.list(c.uid).subscribe({
      next: (ps) => this.prescriptions.set(ps),
      error: () => { /* keep existing */ }
    });
  }

  // ----- Display helpers -------------------------------------------------

  orderStatusBadgeClass(s: ClinicalOrderStatus): string {
    return 'badge ' + (this.orderStatuses.find((x) => x.value === s)?.badgeClass ?? '');
  }
  orderStatusLabel(s: ClinicalOrderStatus): string {
    return this.orderStatuses.find((x) => x.value === s)?.label ?? s;
  }
  orderKindLabel(k: ClinicalOrderKind): string {
    return this.orderKinds.find((x) => x.value === k)?.label ?? k;
  }
  orderKindIcon(k: ClinicalOrderKind): string {
    return this.orderKinds.find((x) => x.value === k)?.icon ?? 'bi-card-list';
  }
  urgencyBadgeClass(u: OrderUrgency): string {
    return 'badge ' + (this.orderUrgencies.find((x) => x.value === u)?.badgeClass ?? '');
  }
  prescriptionStatusBadgeClass(s: PrescriptionStatus): string {
    return 'badge ' + (this.prescriptionStatuses.find((x) => x.value === s)?.badgeClass ?? '');
  }
  prescriptionStatusLabel(s: PrescriptionStatus): string {
    return this.prescriptionStatuses.find((x) => x.value === s)?.label ?? s;
  }

  private setNote(note: ClinicalNote | null): void {
    this.note.set(note);
    if (note) {
      this.noteForm.patchValue({
        chiefComplaint: note.chiefComplaint ?? '',
        historyOfPresentingIllness: note.historyOfPresentingIllness ?? '',
        pastMedicalHistory: note.pastMedicalHistory ?? '',
        examination: note.examination ?? '',
        assessment: note.assessment ?? '',
        plan: note.plan ?? ''
      }, { emitEvent: false });
    }
    this.noteDirty.set(false);
  }

  statusBadgeClass(s: ConsultationStatus): string {
    return 'badge ' + (this.statuses.find((x) => x.value === s)?.badgeClass ?? '');
  }

  statusLabel(s: ConsultationStatus): string {
    return this.statuses.find((x) => x.value === s)?.label ?? s;
  }

  diagnosisBadgeClass(k: DiagnosisKind): string {
    return 'badge ' + (this.diagnosisKinds.find((x) => x.value === k)?.badgeClass ?? '');
  }

  formatBp(v: PatientVitals): string {
    if (v.bloodPressureSystolic == null || v.bloodPressureDiastolic == null) return '—';
    return `${v.bloodPressureSystolic} / ${v.bloodPressureDiastolic}`;
  }

  // ----- Billing ---------------------------------------------------------

  generateInvoice(): void {
    const c = this.consultation();
    if (!c) return;
    this.invoiceLoading.set(true);
    this.errorMessage.set(null);
    this.invoiceService.generateForConsultation(c.uid)
      .pipe(finalize(() => this.invoiceLoading.set(false)))
      .subscribe({
        next: (i) => this.invoice.set(i),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not generate invoice.')
      });
  }

  issueInvoice(): void {
    const i = this.invoice();
    if (!i) return;
    this.invoiceService.issue(i.uid).subscribe({
      next: (updated) => this.invoice.set(updated),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not issue invoice.')
    });
  }

  cancelInvoice(): void {
    const i = this.invoice();
    if (!i) return;
    const reason = globalThis.prompt('Reason for cancelling this invoice?')?.trim() ?? null;
    this.invoiceService.cancel(i.uid, reason).subscribe({
      next: (updated) => this.invoice.set(updated),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not cancel invoice.')
    });
  }

  recordInvoicePayment(): void {
    const i = this.invoice();
    if (!i) return;
    const ref = this.modal.open(RecordPaymentComponent, { size: 'lg', backdrop: 'static' });
    (ref.componentInstance as RecordPaymentComponent).invoice = i;
    ref.closed.subscribe((updated: Invoice | undefined) => {
      if (updated) this.invoice.set(updated);
    });
  }

  invoiceStatusBadgeClass(s: InvoiceStatus): string {
    return 'badge ' + (this.invoiceStatuses.find((x) => x.value === s)?.badgeClass ?? '');
  }
  invoiceStatusLabel(s: InvoiceStatus): string {
    return this.invoiceStatuses.find((x) => x.value === s)?.label ?? s;
  }
}

function emptyToNull(v: string | null | undefined): string | null {
  if (v == null) return null;
  const t = v.trim();
  return t === '' ? null : t;
}
