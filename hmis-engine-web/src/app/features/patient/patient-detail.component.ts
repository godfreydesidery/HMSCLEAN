import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize, forkJoin } from 'rxjs';

import { ConsultationService } from '../encounter/consultation/consultation.service';
import {
  CONSULTATION_STATUSES, ConsultationStatus, ConsultationSummary
} from '../encounter/consultation/consultation.types';
import { AddOrderComponent } from '../encounter/order/add-order.component';
import { AddPrescriptionComponent } from '../encounter/prescription/add-prescription.component';
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
  private readonly modal = inject(NgbModal);

  readonly statuses = CONSULTATION_STATUSES;
  readonly patient = signal<Patient | null>(null);
  readonly recentConsultations = signal<ConsultationSummary[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

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
      recent: this.consultationService.recentForPatient(uid)
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: ({ patient, recent }) => {
        this.patient.set(patient);
        this.recentConsultations.set(recent);
      },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load patient.')
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

  raiseOutsiderOrder(): void {
    const p = this.patient();
    if (!p) return;
    const ref = this.modal.open(AddOrderComponent, { size: 'lg', backdrop: 'static' });
    (ref.componentInstance as AddOrderComponent).outsiderPatientUid = p.uid;
  }

  raiseOutsiderPrescription(): void {
    const p = this.patient();
    if (!p) return;
    const ref = this.modal.open(AddPrescriptionComponent, { size: 'lg', backdrop: 'static' });
    (ref.componentInstance as AddPrescriptionComponent).outsiderPatientUid = p.uid;
  }

  startConsultation(): void {
    const p = this.patient();
    if (!p) return;
    void this.router.navigate(['/encounters', 'consultations', 'new'], {
      queryParams: { patientUid: p.uid }
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
