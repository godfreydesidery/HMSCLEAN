import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

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

  readonly patient = signal<Patient | null>(null);
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
    if (uid) {
      this.patientService.findByUid(uid)
        .pipe(finalize(() => this.loading.set(false)))
        .subscribe({
          next: (p) => this.patient.set(p),
          error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load patient.')
        });
    } else {
      this.loading.set(false);
      this.errorMessage.set('Missing patient identifier.');
    }
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

  genderLabel(g: Gender): string { return GENDERS.find((x) => x.value === g)?.label ?? g; }
  typeLabel(t: PatientType): string { return PATIENT_TYPES.find((x) => x.value === t)?.label ?? t; }
  paymentLabel(p: PaymentType): string { return PAYMENT_TYPES.find((x) => x.value === p)?.label ?? p; }
}
