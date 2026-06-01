import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize, forkJoin } from 'rxjs';

import { InvoiceService } from '../billing/invoice.service';
import { RecordPaymentComponent } from '../billing/record-payment.component';
import { INVOICE_STATUSES, Invoice, InvoiceStatus } from '../billing/invoice.types';
import { ConsultationService } from '../encounter/consultation/consultation.service';
import { SendToDoctorModalComponent } from '../encounter/consultation/send-to-doctor-modal.component';
import {
  CONSULTATION_STATUSES, Consultation, ConsultationStatus, ConsultationSummary
} from '../encounter/consultation/consultation.types';
import { AddOrderComponent } from '../encounter/order/add-order.component';
import { AddPrescriptionComponent } from '../encounter/prescription/add-prescription.component';
import { ChangePaymentTypeComponent } from './change-payment-type.component';
import { PatientService } from './patient.service';
import { GENDERS, Gender, PATIENT_TYPES, PAYMENT_TYPES, Patient, PatientType, PaymentType } from './patient.types';

@Component({
  selector: 'app-patient-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './patient-detail.component.html',
  styleUrl: './patient-detail.component.scss'
})
export class PatientDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly patientService = inject(PatientService);
  private readonly consultationService = inject(ConsultationService);
  private readonly invoiceService = inject(InvoiceService);
  private readonly modal = inject(NgbModal);

  readonly statuses = CONSULTATION_STATUSES;
  readonly invoiceStatuses = INVOICE_STATUSES;
  readonly patient = signal<Patient | null>(null);
  readonly recentConsultations = signal<ConsultationSummary[]>([]);
  readonly outsiderInvoice = signal<Invoice | null>(null);
  readonly registrationInvoice = signal<Invoice | null>(null);
  readonly registrationBusy = signal(false);
  readonly invoiceBusy = signal(false);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly actionMessage = signal<string | null>(null);

  readonly fullName = computed(() => {
    const p = this.patient();
    if (!p) return '';
    return [p.firstName, p.middleName, p.lastName].filter((s) => !!s && s.length > 0).join(' ');
  });

  readonly initials = computed(() => {
    const p = this.patient();
    if (!p) return '?';
    return ((p.firstName?.[0] ?? '') + (p.lastName?.[0] ?? '')).toUpperCase() || '?';
  });

  readonly age = computed(() => {
    const p = this.patient();
    if (!p) return null;
    const birth = new Date(p.dateOfBirth);
    const now = new Date();
    let years = now.getFullYear() - birth.getFullYear();
    const m = now.getMonth() - birth.getMonth();
    if (m < 0 || (m === 0 && now.getDate() < birth.getDate())) years--;
    return years;
  });

  constructor() {
    const uid = this.route.snapshot.paramMap.get('uid');
    if (!uid) {
      this.loading.set(false);
      this.errorMessage.set('Missing patient identifier.');
      return;
    }
    forkJoin({
      patient: this.patientService.findByUid(uid),
      recent: this.consultationService.recentForPatient(uid),
      registration: this.invoiceService.findRegistrationFee(uid)
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: ({ patient, recent, registration }) => {
        this.patient.set(patient);
        this.recentConsultations.set(recent);
        this.registrationInvoice.set(registration);
        if (patient.type === 'OUTSIDER') this.refreshOutsiderInvoice();
      },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load patient.')
    });
  }

  refreshRegistrationFee(): void {
    const p = this.patient();
    if (!p || this.registrationBusy()) return;
    this.registrationBusy.set(true);
    this.invoiceService.ensureRegistrationFee(p.uid)
      .pipe(finalize(() => this.registrationBusy.set(false)))
      .subscribe({
        next: (inv) => { this.registrationInvoice.set(inv); this.actionMessage.set('Registration fee invoice ready.'); },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load registration fee.')
      });
  }

  private refreshOutsiderInvoice(): void {
    const p = this.patient();
    if (!p || p.type !== 'OUTSIDER') {
      this.outsiderInvoice.set(null);
      return;
    }
    this.invoiceService.findCurrentOutsiderDraft(p.uid).subscribe({
      next: (inv) => this.outsiderInvoice.set(inv),
      error: () => { /* leave previous */ }
    });
  }

  edit(): void {
    const p = this.patient();
    if (p) void this.router.navigate(['/patients', p.uid, 'edit']);
  }

  back(): void { void this.router.navigate(['/patients']); }

  toggleActive(): void {
    const p = this.patient();
    if (!p) return;
    this.patientService.setActive(p.uid, !p.active).subscribe({
      next: (updated) => this.patient.set(updated),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not update patient.')
    });
  }

  /**
   * Flip the patient between OUTPATIENT and OUTSIDER routing. Doesn't
   * touch past encounters — only changes what is allowed going forward
   * (consultations are blocked for OUTSIDER, outsider-direct orders /
   * prescriptions are blocked for OUTPATIENT).
   */
  toggleType(): void {
    const p = this.patient();
    if (!p) return;
    const next: PatientType = p.type === 'OUTSIDER' ? 'OUTPATIENT' : 'OUTSIDER';
    const verb = next === 'OUTSIDER' ? 'mark as walk-in (OUTSIDER)' : 'restore to OUTPATIENT';
    if (!globalThis.confirm(`Are you sure you want to ${verb}?`)) return;
    this.patientService.changeType(p.uid, next).subscribe({
      next: (updated) => this.patient.set(updated),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not change patient type.')
    });
  }

  changePaymentType(): void {
    const p = this.patient(); if (!p) return;
    const ref = this.modal.open(ChangePaymentTypeComponent, { backdrop: 'static' });
    (ref.componentInstance as ChangePaymentTypeComponent).patient = p;
    ref.closed.subscribe((updated: Patient | undefined) => { if (updated) this.patient.set(updated); });
  }

  raiseOutsiderOrder(): void {
    const p = this.patient();
    if (!p) return;
    const ref = this.modal.open(AddOrderComponent, { size: 'lg', backdrop: 'static' });
    (ref.componentInstance as AddOrderComponent).outsiderPatientUid = p.uid;
    ref.closed.subscribe((created) => { if (created) this.refreshOutsiderInvoice(); });
  }

  raiseOutsiderPrescription(): void {
    const p = this.patient();
    if (!p) return;
    const ref = this.modal.open(AddPrescriptionComponent, { size: 'lg', backdrop: 'static' });
    (ref.componentInstance as AddPrescriptionComponent).outsiderPatientUid = p.uid;
    ref.closed.subscribe((created) => { if (created) this.refreshOutsiderInvoice(); });
  }

  // ----- outsider billing actions -----------------------------------------

  generateOutsiderInvoice(): void {
    const p = this.patient();
    if (!p) return;
    this.invoiceBusy.set(true);
    this.invoiceService.generateForOutsider(p.uid).subscribe({
      next: (inv) => {
        this.outsiderInvoice.set(inv);
        this.actionMessage.set(`Invoice ${inv.invoiceNo} ready — ${inv.lines.length} line${inv.lines.length === 1 ? '' : 's'}.`);
        this.invoiceBusy.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.message ?? 'Could not generate invoice.');
        this.invoiceBusy.set(false);
      }
    });
  }

  issueOutsiderInvoice(): void {
    const inv = this.outsiderInvoice();
    if (!inv) return;
    this.invoiceBusy.set(true);
    this.invoiceService.issue(inv.uid).subscribe({
      next: (updated) => {
        this.outsiderInvoice.set(updated);
        this.actionMessage.set(`Invoice ${updated.invoiceNo} issued.`);
        this.invoiceBusy.set(false);
      },
      error: (err) => { this.errorMessage.set(err?.error?.message ?? 'Could not issue invoice.'); this.invoiceBusy.set(false); }
    });
  }

  recordOutsiderPayment(): void {
    const inv = this.outsiderInvoice();
    if (!inv) return;
    const ref = this.modal.open(RecordPaymentComponent, { backdrop: 'static' });
    (ref.componentInstance as RecordPaymentComponent).invoice = inv;
    ref.closed.subscribe((updated: Invoice | undefined) => {
      if (updated) {
        // If the invoice is now PAID, it's no longer the "current draft" for
        // this patient — clear so a fresh "Generate" starts a new invoice.
        if (updated.status === 'PAID' || updated.status === 'CANCELLED') {
          this.outsiderInvoice.set(null);
        } else {
          this.outsiderInvoice.set(updated);
        }
        this.actionMessage.set('Payment recorded.');
      }
    });
  }

  cancelOutsiderInvoice(): void {
    const inv = this.outsiderInvoice();
    if (!inv) return;
    const reason = globalThis.prompt('Reason for cancelling this invoice?')?.trim() || null;
    if (reason === null) return;
    this.invoiceBusy.set(true);
    this.invoiceService.cancel(inv.uid, reason).subscribe({
      next: () => {
        this.outsiderInvoice.set(null);
        this.actionMessage.set('Invoice cancelled.');
        this.invoiceBusy.set(false);
      },
      error: (err) => { this.errorMessage.set(err?.error?.message ?? 'Could not cancel invoice.'); this.invoiceBusy.set(false); }
    });
  }

  invoiceStatusBadgeClass(s: InvoiceStatus): string {
    return 'badge ' + (this.invoiceStatuses.find((x) => x.value === s)?.badgeClass ?? '');
  }
  invoiceStatusLabel(s: InvoiceStatus): string {
    return this.invoiceStatuses.find((x) => x.value === s)?.label ?? s;
  }

  /**
   * "Send to doctor" — opens the clinic/clinician picker and auto-creates the
   * consultation (legacy reception action). The doctor then picks it up from
   * the reception queue. Only valid for OUTPATIENTs; OUTSIDERs use direct orders.
   */
  sendToDoctor(): void {
    const p = this.patient();
    if (!p) return;
    const ref = this.modal.open(SendToDoctorModalComponent, { size: 'lg', backdrop: 'static' });
    (ref.componentInstance as SendToDoctorModalComponent).patient = p;
    ref.closed.subscribe((created: Consultation | undefined) => {
      if (!created) return;
      this.actionMessage.set(`Consultation ${created.consultationNo} created — sent to ${created.clinicianName || created.clinicianUsername}.`);
      this.consultationService.recentForPatient(p.uid).subscribe({
        next: (recent) => this.recentConsultations.set(recent),
        error: () => { /* keep previous */ }
      });
    });
  }

  admit(): void {
    const p = this.patient();
    if (!p) return;
    void this.router.navigate(['/encounters', 'admissions', 'new'], {
      queryParams: { patientUid: p.uid }
    });
  }

  genderLabel(g: Gender): string { return GENDERS.find((x) => x.value === g)?.label ?? g; }
  typeLabel(t: PatientType): string { return PATIENT_TYPES.find((x) => x.value === t)?.label ?? t; }
  typeBadgeClass(t: PatientType): string {
    return 'badge ' + (PATIENT_TYPES.find((x) => x.value === t)?.badgeClass ?? '');
  }
  paymentLabel(p: PaymentType): string { return PAYMENT_TYPES.find((x) => x.value === p)?.label ?? p; }

  statusBadgeClass(s: ConsultationStatus): string {
    return 'badge ' + (this.statuses.find((x) => x.value === s)?.badgeClass ?? '');
  }
  statusLabel(s: ConsultationStatus): string {
    return this.statuses.find((x) => x.value === s)?.label ?? s;
  }
}
