import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { ClinicService } from '../../masterdata/clinics/clinic.service';
import { Clinic } from '../../masterdata/clinics/clinic.types';
import { ConsultationService } from './consultation.service';
import { ConsultationTransfer } from './consultation.types';

/**
 * Raise a two-phase transfer (reworked Phase 44): the treating doctor picks a
 * target CLINIC only. The source consultation flips to TRANSFERRED and a PENDING
 * transfer is queued for reception, who later accepts it and chooses the
 * receiving clinician. No clinician is chosen here.
 */
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
  protected readonly activeModal = inject(NgbActiveModal);

  readonly clinics = signal<Clinic[]>([]);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    targetClinicUid: ['', [Validators.required]],
    reason:          ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    this.clinicService.search({ active: true, size: 200, sort: 'name,asc' })
      .subscribe({ next: (r) => this.clinics.set(r.content), error: () => { /* dropdown stays empty */ } });
  }

  submit(): void {
    if (this.submitting()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.consultationService.raiseTransfer(this.sourceUid, {
      targetClinicUid: raw.targetClinicUid,
      reason:          raw.reason?.trim() || null
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (transfer: ConsultationTransfer) => this.activeModal.close(transfer),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not transfer consultation.')
    });
  }
}
