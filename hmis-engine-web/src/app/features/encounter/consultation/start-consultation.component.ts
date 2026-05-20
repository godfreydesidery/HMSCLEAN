import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { debounceTime, distinctUntilChanged, finalize, forkJoin } from 'rxjs';

import { StaffDirectoryService, StaffOption } from '../../../core/directory/staff-directory.service';
import { ClinicService } from '../../masterdata/clinics/clinic.service';
import { Clinic } from '../../masterdata/clinics/clinic.types';
import { InsurancePlanService } from '../../masterdata/insurance-plans/insurance-plan.service';
import { InsurancePlan } from '../../masterdata/insurance-plans/insurance-plan.types';
import { InvoiceService } from '../../billing/invoice.service';
import { PatientService } from '../../patient/patient.service';
import { PAYMENT_TYPES, Patient, PaymentType } from '../../patient/patient.types';
import { ConsultationService } from './consultation.service';
import { ConsultationSummary } from './consultation.types';

/** ULID length — patient uids are 26 chars; used to gate the follow-up lookup. */
const ULID_LENGTH = 26;

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
  private readonly invoiceService = inject(InvoiceService);

  readonly paymentTypes = PAYMENT_TYPES;

  readonly patient = signal<Patient | null>(null);
  readonly clinics = signal<Clinic[]>([]);
  readonly clinicians = signal<StaffOption[]>([]);
  readonly plans = signal<InsurancePlan[]>([]);
  /** Prior COMPLETED consultations for the selected patient (most recent first, top 10). */
  readonly followUpCandidates = signal<ConsultationSummary[]>([]);

  readonly loadingLookups = signal(true);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  /** Set when the booking was refused because a CASH patient has an unpaid registration fee. */
  readonly registrationFeeBlocked = signal(false);
  /** Registration-fee invoice uid for the deep link, when resolvable. */
  readonly registrationInvoiceUid = signal<string | null>(null);

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

    // Refresh the follow-up dropdown whenever a complete patient uid is in play.
    this.form.controls.patientUid.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((uid) => this.loadFollowUpCandidates(uid));

    const patientUid = this.route.snapshot.queryParamMap.get('patientUid');
    if (patientUid) {
      this.form.controls.patientUid.setValue(patientUid);
      this.loadFollowUpCandidates(patientUid);
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

  /** Load the patient's prior COMPLETED consultations for the follow-up picker. */
  private loadFollowUpCandidates(patientUid: string | null | undefined): void {
    const uid = patientUid?.trim() ?? '';
    if (uid.length !== ULID_LENGTH) { this.followUpCandidates.set([]); return; }
    this.consultationService.recentForPatient(uid).subscribe({
      next: (rows) => this.followUpCandidates.set(rows.filter((c) => c.status === 'COMPLETED').slice(0, 10)),
      error: () => this.followUpCandidates.set([])
    });
  }

  /** True when a follow-up uid is set but isn't among the loaded candidates (e.g. pre-filled via query param). */
  get followUpValueMissing(): boolean {
    const v = this.form.controls.followUpOfConsultationUid.value?.trim();
    return !!v && !this.followUpCandidates().some((c) => c.uid === v);
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
    this.registrationFeeBlocked.set(false);
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
      error: (err) => this.handleBookingError(err, raw.patientUid)
    });
  }

  /**
   * The booking gate (RegistrationFeeListeners) rejects CASH patients with an
   * unpaid registration fee via a 422 BUSINESS_RULE error. Surface that as a
   * contextual prompt with a deep-link to settle the fee, rather than a raw
   * error string.
   */
  private handleBookingError(err: unknown, patientUid: string): void {
    const e = err as { status?: number; error?: { message?: string } };
    const message = e?.error?.message ?? '';
    if (e?.status === 422 && /registration fee/i.test(message)) {
      this.registrationFeeBlocked.set(true);
      this.invoiceService.findRegistrationFee(patientUid).subscribe({
        next: (inv) => this.registrationInvoiceUid.set(inv?.uid ?? null),
        error: () => this.registrationInvoiceUid.set(null)
      });
      return;
    }
    this.errorMessage.set(message || 'Could not start consultation.');
  }

  /** Open the patient's registration-fee invoice so the cashier can settle it. */
  openRegistrationInvoice(): void {
    const uid = this.registrationInvoiceUid();
    if (uid) void this.router.navigate(['/billing', uid]);
  }

  cancel(): void { void this.router.navigate(['/encounters', 'consultations']); }
}
