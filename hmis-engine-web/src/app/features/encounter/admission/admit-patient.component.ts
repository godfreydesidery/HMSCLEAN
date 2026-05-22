import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';

import { StaffDirectoryService, StaffOption } from '../../../core/directory/staff-directory.service';
import { PatientSearchComponent } from '../../../shared/patient-search/patient-search.component';
import { InsurancePlanService } from '../../masterdata/insurance-plans/insurance-plan.service';
import { InsurancePlan } from '../../masterdata/insurance-plans/insurance-plan.types';
import { WardService } from '../../masterdata/wards/ward.service';
import { Ward } from '../../masterdata/wards/ward.types';
import { PAYMENT_TYPES, Patient, PaymentType } from '../../patient/patient.types';
import { ConsultationService } from '../consultation/consultation.service';
import { ConsultationSummary } from '../consultation/consultation.types';
import { AdmissionService } from './admission.service';

@Component({
  selector: 'app-admit-patient',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PatientSearchComponent],
  templateUrl: './admit-patient.component.html',
  styleUrl: './admit-patient.component.scss'
})
export class AdmitPatientComponent {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly admissionService = inject(AdmissionService);
  private readonly wardService = inject(WardService);
  private readonly planService = inject(InsurancePlanService);
  private readonly staffService = inject(StaffDirectoryService);
  private readonly consultationService = inject(ConsultationService);

  readonly paymentTypes = PAYMENT_TYPES;

  readonly patient = signal<Patient | null>(null);
  readonly wards = signal<Ward[]>([]);
  readonly clinicians = signal<StaffOption[]>([]);
  readonly plans = signal<InsurancePlan[]>([]);
  /** The selected patient's recent consultations, for the optional linkage dropdown. */
  readonly consultationCandidates = signal<ConsultationSummary[]>([]);
  /** Patient uid from a ?patientUid deep-link, handed to the patient-search to preload. */
  readonly initialPatientUid = this.route.snapshot.queryParamMap.get('patientUid');

  readonly loadingLookups = signal(true);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    patientUid: ['', [Validators.required]],
    wardUid: ['', [Validators.required]],
    bedLabel: ['', [Validators.maxLength(32)]],
    admittingClinicianUsername: ['', [Validators.required]],
    paymentType: ['CASH' as PaymentType, [Validators.required]],
    insurancePlanUid: [''],
    consultationUid: [''],
    admissionReason: ['', [Validators.maxLength(500)]]
  });

  constructor() {
    forkJoin({
      wards: this.wardService.search({ active: true, size: 200, sort: 'name,asc' }),
      clinicians: this.staffService.byRole('CLINICIAN'),
      plans: this.planService.search({ active: true, size: 200, sort: 'name,asc' })
    }).pipe(finalize(() => this.loadingLookups.set(false))).subscribe({
      next: ({ wards, clinicians, plans }) => {
        this.wards.set(wards.content);
        this.clinicians.set(clinicians);
        this.plans.set(plans.content);
      },
      error: () => this.errorMessage.set('Could not load lookups.')
    });

    const consultationUid = this.route.snapshot.queryParamMap.get('consultationUid');
    if (consultationUid) {
      this.form.controls.consultationUid.setValue(consultationUid);
    }
  }

  /** Patient chosen via the search typeahead (or preloaded from the deep-link). */
  onPatientSelected(p: Patient | null): void {
    this.patient.set(p);
    this.form.controls.patientUid.setValue(p?.uid ?? '');
    if (p) {
      this.form.patchValue({
        paymentType: p.paymentType,
        insurancePlanUid: p.insurancePlanUid ?? ''
      });
      this.consultationService.recentForPatient(p.uid).subscribe({
        next: (rows) => this.consultationCandidates.set(rows.slice(0, 10)),
        error: () => this.consultationCandidates.set([])
      });
    } else {
      this.consultationCandidates.set([]);
      this.form.controls.consultationUid.setValue('');
    }
  }

  /** True when a consultation uid is set but isn't among the loaded candidates (e.g. deep-link). */
  get consultationValueMissing(): boolean {
    const v = this.form.controls.consultationUid.value?.trim();
    return !!v && !this.consultationCandidates().some((c) => c.uid === v);
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
    this.admissionService.admit({
      patientUid: raw.patientUid,
      wardUid: raw.wardUid,
      bedLabel: raw.bedLabel?.trim() || null,
      admittingClinicianUsername: raw.admittingClinicianUsername,
      paymentType: raw.paymentType,
      insurancePlanUid: raw.insurancePlanUid || null,
      consultationUid: raw.consultationUid || null,
      admissionReason: raw.admissionReason?.trim() || null
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (a) => void this.router.navigate(['/encounters', 'admissions', a.uid]),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not admit patient.')
    });
  }

  cancel(): void { void this.router.navigate(['/encounters', 'admissions']); }
}
