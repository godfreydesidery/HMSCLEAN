import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';

import { ConsultationService } from '../encounter/consultation/consultation.service';
import {
  CONSULTATION_STATUSES, ConsultationStatus, ConsultationSummary
} from '../encounter/consultation/consultation.types';
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
  paymentLabel(p: PaymentType): string { return PAYMENT_TYPES.find((x) => x.value === p)?.label ?? p; }

  statusBadgeClass(s: ConsultationStatus): string {
    return 'badge ' + (this.statuses.find((x) => x.value === s)?.badgeClass ?? '');
  }
  statusLabel(s: ConsultationStatus): string {
    return this.statuses.find((x) => x.value === s)?.label ?? s;
  }
}
