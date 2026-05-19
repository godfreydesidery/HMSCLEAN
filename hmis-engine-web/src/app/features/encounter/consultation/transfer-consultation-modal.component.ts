import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { StaffDirectoryService, StaffOption } from '../../../core/directory/staff-directory.service';
import { ClinicService } from '../../masterdata/clinics/clinic.service';
import { Clinic } from '../../masterdata/clinics/clinic.types';
import { ConsultationService } from './consultation.service';
import { Consultation } from './consultation.types';

@Component({
  selector: 'app-transfer-consultation-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './transfer-consultation-modal.component.html'
})
export class TransferConsultationModalComponent implements OnInit {
  @Input({ required: true }) sourceUid!: string;

  private readonly fb = inject(FormBuilder);
  private readonly consultationService = inject(ConsultationService);
  private readonly clinicService = inject(ClinicService);
  private readonly staffService = inject(StaffDirectoryService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly clinics = signal<Clinic[]>([]);
  readonly clinicians = signal<StaffOption[]>([]);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    targetClinicUid:         ['', [Validators.required]],
    targetClinicianUsername: ['', [Validators.required]],
    reason:                  ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    this.clinicService.search({ active: true, size: 200, sort: 'name,asc' })
      .subscribe({ next: (r) => this.clinics.set(r.content), error: () => { /* dropdown stays empty */ } });
    this.staffService.byRole('CLINICIAN')
      .subscribe({ next: (rows) => this.clinicians.set(rows), error: () => { /* */ } });
  }

  submit(): void {
    if (this.submitting()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.consultationService.transfer(this.sourceUid, {
      targetClinicUid:         raw.targetClinicUid,
      targetClinicianUsername: raw.targetClinicianUsername,
      reason:                  raw.reason?.trim() || null
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (receiver: Consultation) => this.activeModal.close(receiver),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not transfer consultation.')
    });
  }
}
