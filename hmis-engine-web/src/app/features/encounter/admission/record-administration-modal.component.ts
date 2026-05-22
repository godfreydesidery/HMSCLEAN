import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { PrescriptionService } from '../prescription/prescription.service';
import { Prescription } from '../prescription/prescription.types';
import { MedicationAdminService } from './medication-admin.service';

/**
 * Record one bedside dose against an admission's prescription (the nursing MAR,
 * PROCESS_MISMATCHES.md M15). Prescriptions are loaded from the admission's
 * source consultation.
 */
@Component({
  selector: 'app-record-administration-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './record-administration-modal.component.html'
})
export class RecordAdministrationModalComponent implements OnInit {
  @Input({ required: true }) admissionUid!: string;
  @Input() consultationUid: string | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(MedicationAdminService);
  private readonly prescriptionService = inject(PrescriptionService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly prescriptions = signal<Prescription[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    prescriptionUid: ['', [Validators.required]],
    doseGiven: ['', [Validators.required, Validators.maxLength(120)]],
    route: ['', [Validators.maxLength(80)]],
    patientResponse: ['', [Validators.maxLength(500)]],
    notes: ['', [Validators.maxLength(1000)]]
  });

  ngOnInit(): void {
    if (!this.consultationUid) { this.loading.set(false); return; }
    this.prescriptionService.list(this.consultationUid)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (rows) => this.prescriptions.set(rows),
        error: () => this.errorMessage.set('Could not load prescriptions.')
      });
  }

  /** Prefill the dose + route from the selected prescription. */
  onPrescriptionChange(uid: string): void {
    const rx = this.prescriptions().find((p) => p.uid === uid);
    if (rx) {
      this.form.patchValue({ doseGiven: rx.dose ?? '' });
    }
  }

  submit(): void {
    if (this.busy()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.busy.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.service.record(this.admissionUid, {
      prescriptionUid: raw.prescriptionUid,
      doseGiven: raw.doseGiven.trim(),
      route: raw.route.trim() || null,
      patientResponse: raw.patientResponse.trim() || null,
      notes: raw.notes.trim() || null,
      administeredAt: null
    }).pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (rec) => this.activeModal.close(rec),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not record administration.')
    });
  }
}
