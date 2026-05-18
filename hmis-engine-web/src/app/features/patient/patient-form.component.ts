import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize, startWith } from 'rxjs';

import { InsurancePlanService } from '../masterdata/insurance-plans/insurance-plan.service';
import { InsurancePlan } from '../masterdata/insurance-plans/insurance-plan.types';
import { PatientService } from './patient.service';
import {
  GENDERS, Gender, PATIENT_TYPES, PAYMENT_TYPES, Patient, PatientType, PaymentType
} from './patient.types';

@Component({
  selector: 'app-patient-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './patient-form.component.html',
  styleUrl: './patient-form.component.scss'
})
export class PatientFormComponent implements OnInit {
  /** Pass when editing — undefined for registration. */
  readonly existing = input<Patient | null>(null);

  private readonly fb = inject(FormBuilder);
  private readonly patientService = inject(PatientService);
  private readonly insurancePlanService = inject(InsurancePlanService);
  private readonly router = inject(Router);

  readonly genders = GENDERS;
  readonly paymentTypes = PAYMENT_TYPES;
  readonly patientTypes = PATIENT_TYPES;

  readonly insurancePlans = signal<InsurancePlan[]>([]);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(80)]],
    middleName: ['', [Validators.maxLength(80)]],
    lastName: ['', [Validators.required, Validators.maxLength(80)]],

    dateOfBirth: ['', [Validators.required]],
    gender: ['MALE' as Gender, [Validators.required]],
    type: ['OUTPATIENT' as PatientType, [Validators.required]],
    paymentType: ['CASH' as PaymentType, [Validators.required]],

    insurancePlanUid: [''],
    membershipNo: ['', [Validators.maxLength(64)]],

    phoneNo: ['', [Validators.maxLength(40)]],
    email: ['', [Validators.email, Validators.maxLength(120)]],
    address: ['', [Validators.maxLength(255)]],
    nationality: ['', [Validators.maxLength(80)]],
    nationalId: ['', [Validators.maxLength(64)]],
    passportNo: ['', [Validators.maxLength(64)]],

    kinFullName: ['', [Validators.maxLength(160)]],
    kinRelationship: ['', [Validators.maxLength(80)]],
    kinPhoneNo: ['', [Validators.maxLength(40)]]
  });

  /** True when the chosen payment type requires an insurance plan. */
  readonly insuranceRequired = toSignal(
    this.form.controls.paymentType.valueChanges.pipe(
      startWith(this.form.controls.paymentType.value),
      takeUntilDestroyed()
    ),
    { initialValue: this.form.controls.paymentType.value }
  );

  readonly insuranceRequiredBool = computed(() => {
    const pt = this.insuranceRequired();
    return pt === 'INSURANCE' || pt === 'MIXED';
  });

  ngOnInit(): void {
    this.insurancePlanService.search({ active: true, size: 200, sort: 'name,asc' }).subscribe({
      next: (res) => this.insurancePlans.set(res.content),
      error: () => { /* ignore — dropdown stays empty */ }
    });

    const e = this.existing();
    if (e) {
      this.form.patchValue({
        firstName: e.firstName,
        middleName: e.middleName ?? '',
        lastName: e.lastName,
        dateOfBirth: e.dateOfBirth,
        gender: e.gender,
        type: e.type,
        paymentType: e.paymentType,
        insurancePlanUid: e.insurancePlanUid ?? '',
        membershipNo: e.membershipNo ?? '',
        phoneNo: e.phoneNo ?? '',
        email: e.email ?? '',
        address: e.address ?? '',
        nationality: e.nationality ?? '',
        nationalId: e.nationalId ?? '',
        passportNo: e.passportNo ?? '',
        kinFullName: e.kinFullName ?? '',
        kinRelationship: e.kinRelationship ?? '',
        kinPhoneNo: e.kinPhoneNo ?? ''
      });
    }
  }

  get isEdit(): boolean { return !!this.existing(); }
  get title(): string { return this.isEdit ? 'Edit patient' : 'Register patient'; }

  submit(): void {
    if (this.submitting()) return;
    if (this.insuranceRequiredBool() && !this.form.controls.insurancePlanUid.value) {
      this.form.controls.insurancePlanUid.setErrors({ required: true });
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const payload = {
      firstName: raw.firstName.trim(),
      middleName: emptyToNull(raw.middleName),
      lastName: raw.lastName.trim(),
      dateOfBirth: raw.dateOfBirth,
      gender: raw.gender,
      type: raw.type,
      paymentType: raw.paymentType,
      insurancePlanUid: emptyToNull(raw.insurancePlanUid),
      membershipNo: emptyToNull(raw.membershipNo),
      phoneNo: emptyToNull(raw.phoneNo),
      email: emptyToNull(raw.email),
      address: emptyToNull(raw.address),
      nationality: emptyToNull(raw.nationality),
      nationalId: emptyToNull(raw.nationalId),
      passportNo: emptyToNull(raw.passportNo),
      kinFullName: emptyToNull(raw.kinFullName),
      kinRelationship: emptyToNull(raw.kinRelationship),
      kinPhoneNo: emptyToNull(raw.kinPhoneNo)
    };

    const existing = this.existing();
    const req$ = existing
      ? this.patientService.update(existing.uid, payload)
      : this.patientService.register(payload);

    req$.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: () => void this.router.navigate(['/patients']),
      error: (err) => {
        const fieldErrors: { field: string; message: string }[] = err?.error?.errors ?? [];
        const summary = fieldErrors.map((fe) => `${fe.field}: ${fe.message}`).join('; ');
        this.errorMessage.set(summary || err?.error?.message || 'Could not save patient.');
      }
    });
  }

  cancel(): void {
    void this.router.navigate(['/patients']);
  }
}

function emptyToNull(v: string | null | undefined): string | null {
  if (v == null) return null;
  const t = v.trim();
  return t === '' ? null : t;
}
