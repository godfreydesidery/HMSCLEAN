import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';

import { StaffDirectoryService, StaffOption } from '../../../core/directory/staff-directory.service';
import { ClinicService } from '../../masterdata/clinics/clinic.service';
import { Clinic } from '../../masterdata/clinics/clinic.types';
import { InsurancePlanService } from '../../masterdata/insurance-plans/insurance-plan.service';
import { InsurancePlan } from '../../masterdata/insurance-plans/insurance-plan.types';
import { PatientService } from '../../patient/patient.service';
import { PAYMENT_TYPES, Patient, PaymentType } from '../../patient/patient.types';
import { ConsultationService } from './consultation.service';

@Component({
  selector: 'app-start-consultation',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './start-consultation.component.html',
  styleUrl: './start-consultation.component.scss'
})
export class StartConsultationComponent {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly consultationService = inject(ConsultationService);
  private readonly patientService = inject(PatientService);
  private readonly clinicService = inject(ClinicService);
  private readonly planService = inject(InsurancePlanService);
  private readonly staffService = inject(StaffDirectoryService);

  readonly paymentTypes = PAYMENT_TYPES;

  readonly patient = signal<Patient | null>(null);
  readonly clinics = signal<Clinic[]>([]);
  readonly clinicians = signal<StaffOption[]>([]);
  readonly plans = signal<InsurancePlan[]>([]);

  readonly loadingLookups = signal(true);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    patientUid: ['', [Validators.required]],
    clinicUid: ['', [Validators.required]],
    clinicianUsername: ['', [Validators.required]],
    paymentType: ['CASH' as PaymentType, [Validators.required]],
    insurancePlanUid: [''],
    reason: ['', [Validators.maxLength(500)]],
    /** Pre-filled when the route is opened with ?followUpOf=<uid>. */
    followUpOfConsultationUid: ['', [Validators.minLength(26), Validators.maxLength(26)]]
  });

  constructor() {
    forkJoin({
      clinics: this.clinicService.search({ active: true, size: 200, sort: 'name,asc' }),
      clinicians: this.staffService.byRole('CLINICIAN'),
      plans: this.planService.search({ active: true, size: 200, sort: 'name,asc' })
    }).pipe(finalize(() => this.loadingLookups.set(false))).subscribe({
      next: ({ clinics, clinicians, plans }) => {
        this.clinics.set(clinics.content);
        this.clinicians.set(clinicians);
        this.plans.set(plans.content);
      },
      error: () => this.errorMessage.set('Could not load lookups.')
    });

    const followUpOf = this.route.snapshot.queryParamMap.get('followUpOf');
    if (followUpOf) {
      this.form.controls.followUpOfConsultationUid.setValue(followUpOf);
    }

    const patientUid = this.route.snapshot.queryParamMap.get('patientUid');
    if (patientUid) {
      this.form.controls.patientUid.setValue(patientUid);
      this.patientService.findByUid(patientUid).subscribe({
        next: (p) => {
          this.patient.set(p);
          this.form.patchValue({
            paymentType: p.paymentType,
            insurancePlanUid: p.insurancePlanUid ?? ''
          });
        },
        error: () => this.errorMessage.set('Could not load patient details.')
      });
    }
  }

  get insuranceRequired(): boolean {
    const pt = this.form.controls.paymentType.value;
    return pt === 'INSURANCE' || pt === 'MIXED';
  }

  submit(): void {
    if (this.submitting()) return;
    if (this.insuranceRequired && !this.form.controls.insurancePlanUid.value) {
      this.form.controls.insurancePlanUid.setErrors({ required: true });
    }
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.consultationService.book({
      patientUid: raw.patientUid,
      clinicUid: raw.clinicUid,
      clinicianUsername: raw.clinicianUsername,
      paymentType: raw.paymentType,
      insurancePlanUid: raw.insurancePlanUid || null,
      reason: raw.reason?.trim() || null,
      followUpOfConsultationUid: raw.followUpOfConsultationUid?.trim() || null
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (c) => void this.router.navigate(['/encounters', 'consultations', c.uid]),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not start consultation.')
    });
  }

  cancel(): void { void this.router.navigate(['/encounters', 'consultations']); }
}
