import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { InsurancePlanService } from '../masterdata/insurance-plans/insurance-plan.service';
import { InsurancePlan } from '../masterdata/insurance-plans/insurance-plan.types';
import { PatientService } from './patient.service';
import { PAYMENT_TYPES, Patient, PaymentType, UpdatePatientRequest } from './patient.types';

/**
 * Reception quick-action (legacy change_payment_type): switch a patient's payment basis
 * for this visit — CASH ↔ INSURANCE / MIXED with a plan + membership — without opening the
 * full demographics edit. Reuses the patient update endpoint (which enforces the
 * active-encounter gate); only the payment fields change.
 */
@Component({
  selector: 'app-change-payment-type',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './change-payment-type.component.html'
})
export class ChangePaymentTypeComponent implements OnInit {
  @Input({ required: true }) patient!: Patient;

  private readonly fb = inject(FormBuilder);
  private readonly patientService = inject(PatientService);
  private readonly planService = inject(InsurancePlanService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly paymentTypes = PAYMENT_TYPES;
  readonly plans = signal<InsurancePlan[]>([]);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    paymentType: ['CASH' as PaymentType, [Validators.required]],
    insurancePlanUid: [''],
    membershipNo: ['', [Validators.maxLength(64)]]
  });

  ngOnInit(): void {
    this.form.patchValue({
      paymentType: this.patient.paymentType,
      insurancePlanUid: this.patient.insurancePlanUid ?? '',
      membershipNo: this.patient.membershipNo ?? ''
    });
    this.planService.search({ active: true, size: 200, sort: 'name,asc' }).subscribe({
      next: (page) => this.plans.set(page.content),
      error: () => this.plans.set([])
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
    const p = this.patient;
    // Full update with only the payment fields changed (UpdatePatientRequest = the create shape).
    const payload: UpdatePatientRequest = {
      firstName: p.firstName, middleName: p.middleName, lastName: p.lastName,
      dateOfBirth: p.dateOfBirth, gender: p.gender, type: p.type,
      paymentType: raw.paymentType,
      insurancePlanUid: this.insuranceRequired ? (raw.insurancePlanUid || null) : null,
      membershipNo: this.insuranceRequired ? (raw.membershipNo?.trim() || null) : null,
      phoneNo: p.phoneNo, email: p.email, address: p.address,
      nationality: p.nationality, nationalId: p.nationalId, passportNo: p.passportNo,
      kinFullName: p.kinFullName, kinRelationship: p.kinRelationship, kinPhoneNo: p.kinPhoneNo,
      kin2FullName: p.kin2FullName, kin2Relationship: p.kin2Relationship, kin2PhoneNo: p.kin2PhoneNo,
      kin3FullName: p.kin3FullName, kin3Relationship: p.kin3Relationship, kin3PhoneNo: p.kin3PhoneNo
    };
    this.patientService.update(p.uid, payload).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (updated) => this.activeModal.close(updated),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not change the payment type.')
    });
  }
}
