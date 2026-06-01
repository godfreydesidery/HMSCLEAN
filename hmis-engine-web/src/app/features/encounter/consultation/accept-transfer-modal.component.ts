import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { ClinicCliniciansService } from '../../masterdata/clinics/clinic-clinicians.service';
import { ClinicClinician } from '../../masterdata/clinics/clinic-clinicians.types';
import { ConsultationService } from './consultation.service';
import { Consultation, ConsultationTransfer } from './consultation.types';

/**
 * Reception accepts a PENDING transfer, choosing the receiving clinician from the
 * target clinic's affiliated clinicians. On success the receiving consultation is
 * booked and returned.
 */
@Component({
  selector: 'app-accept-transfer-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './accept-transfer-modal.component.html'
})
export class AcceptTransferModalComponent implements OnInit {
  @Input({ required: true }) transfer!: ConsultationTransfer;

  private readonly fb = inject(FormBuilder);
  private readonly consultationService = inject(ConsultationService);
  private readonly clinicCliniciansService = inject(ClinicCliniciansService);
  protected readonly activeModal = inject(NgbActiveModal);

  /** Clinicians affiliated with the transfer's target clinic. */
  readonly clinicians = signal<ClinicClinician[]>([]);
  readonly loadingClinicians = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    clinicianUsername: ['', [Validators.required]]
  });

  ngOnInit(): void {
    this.loadingClinicians.set(true);
    this.clinicCliniciansService.list(this.transfer.targetClinicUid)
      .pipe(finalize(() => this.loadingClinicians.set(false)))
      .subscribe({
        next: (rows) => this.clinicians.set(rows),
        error: () => this.errorMessage.set('Could not load clinicians for the target clinic.')
      });
  }

  submit(): void {
    if (this.submitting()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.consultationService.acceptTransfer(this.transfer.uid, {
      clinicianUsername: this.form.getRawValue().clinicianUsername
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (consultation: Consultation) => this.activeModal.close(consultation),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not accept the transfer.')
    });
  }
}
