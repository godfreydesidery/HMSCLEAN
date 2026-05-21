import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize, forkJoin } from 'rxjs';

import { StaffDirectoryService, StaffOption } from '../../../core/directory/staff-directory.service';
import { ClinicService } from '../../masterdata/clinics/clinic.service';
import { Clinic } from '../../masterdata/clinics/clinic.types';
import { InsurancePlanService } from '../../masterdata/insurance-plans/insurance-plan.service';
import { InsurancePlan } from '../../masterdata/insurance-plans/insurance-plan.types';
import { PAYMENT_TYPES, Patient, PaymentType } from '../../patient/patient.types';
import { ConsultationService } from './consultation.service';
import { Consultation, ConsultationSummary } from './consultation.types';

/**
 * "Send to doctor" — the legacy reception action. Auto-creates the consultation
 * (and, via the backend, the consultation-fee bill) so the doctor can pick it
 * up from the reception queue. Opened from the patient record.
 */
@Component({
  selector: 'app-send-to-doctor-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './send-to-doctor-modal.component.html'
})
export class SendToDoctorModalComponent implements OnInit {
  @Input({ required: true }) patient!: Patient;

  private readonly fb = inject(FormBuilder);
  private readonly consultationService = inject(ConsultationService);
  private readonly clinicService = inject(ClinicService);
  private readonly planService = inject(InsurancePlanService);
  private readonly staffService = inject(StaffDirectoryService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly paymentTypes = PAYMENT_TYPES;
  readonly clinics = signal<Clinic[]>([]);
  readonly clinicians = signal<StaffOption[]>([]);
  readonly plans = signal<InsurancePlan[]>([]);
  readonly followUpCandidates = signal<ConsultationSummary[]>([]);
  readonly loadingLookups = signal(true);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    clinicUid:                 ['', [Validators.required]],
    clinicianUsername:         ['', [Validators.required]],
    paymentType:               ['CASH' as PaymentType, [Validators.required]],
    insurancePlanUid:          [''],
    reason:                    ['', [Validators.maxLength(500)]],
    followUpOfConsultationUid: ['']
  });

  ngOnInit(): void {
    this.form.patchValue({
      paymentType: this.patient.paymentType,
      insurancePlanUid: this.patient.insurancePlanUid ?? ''
    });
    forkJoin({
      clinics: this.clinicService.search({ active: true, size: 200, sort: 'name,asc' }),
      clinicians: this.staffService.byRole('CLINICIAN'),
      plans: this.planService.search({ active: true, size: 200, sort: 'name,asc' }),
      recent: this.consultationService.recentForPatient(this.patient.uid)
    }).pipe(finalize(() => this.loadingLookups.set(false))).subscribe({
      next: ({ clinics, clinicians, plans, recent }) => {
        this.clinics.set(clinics.content);
        this.clinicians.set(clinicians);
        this.plans.set(plans.content);
        this.followUpCandidates.set(recent.filter((c) => c.status === 'COMPLETED').slice(0, 10));
      },
      error: () => this.errorMessage.set('Could not load lookups.')
    });
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
      patientUid: this.patient.uid,
      clinicUid: raw.clinicUid,
      clinicianUsername: raw.clinicianUsername,
      paymentType: raw.paymentType,
      insurancePlanUid: raw.insurancePlanUid || null,
      reason: raw.reason?.trim() || null,
      followUpOfConsultationUid: raw.followUpOfConsultationUid?.trim() || null
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (c: Consultation) => this.activeModal.close(c),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not send the patient to a doctor.')
    });
  }
}
