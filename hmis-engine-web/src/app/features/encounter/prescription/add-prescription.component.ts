import { CommonModule } from '@angular/common';
import { Component, DestroyRef, Input, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { debounceTime, finalize } from 'rxjs';

import { MedicineService } from '../../masterdata/medicines/medicine.service';
import { Medicine } from '../../masterdata/medicines/medicine.types';
import { PrescriptionService } from './prescription.service';
import { PrescribingAlert } from './prescription.types';

@Component({
  selector: 'app-add-prescription',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './add-prescription.component.html'
})
export class AddPrescriptionComponent implements OnInit {
  /** Set for the OUTPATIENT pathway. */
  @Input() consultationUid: string | null = null;
  /** Set for the OUTSIDER pathway (direct-to-patient retail / OTC prescription). */
  @Input() outsiderPatientUid: string | null = null;
  /** Patient whose dispense history drives the advisory pre-prescribe alerts. */
  @Input() patientUid: string | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly prescriptionService = inject(PrescriptionService);
  private readonly medicineService = inject(MedicineService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly medicines = signal<Medicine[]>([]);
  readonly loadingMedicines = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  /** Advisory, non-blocking pre-prescribe alerts for the selected patient/medicine. */
  readonly alerts = signal<PrescribingAlert[]>([]);

  readonly form = this.fb.nonNullable.group({
    medicineUid:  ['', [Validators.required]],
    dose:         ['', [Validators.required, Validators.maxLength(80)]],
    frequency:    ['', [Validators.required, Validators.maxLength(80)]],
    durationDays: [null as number | null, [Validators.min(0)]],
    quantity:     [null as number | null, [Validators.min(0)]],
    instructions: ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    this.loadingMedicines.set(true);
    this.medicineService.search({ active: true, size: 500, sort: 'name,asc' })
      .pipe(finalize(() => this.loadingMedicines.set(false)))
      .subscribe({
        next: (res) => this.medicines.set(res.content),
        error: () => this.errorMessage.set('Could not load medicines catalogue.')
      });

    // Advisory pre-prescribe check: react to medicine selection, debounced. Never blocks Save.
    this.form.controls.medicineUid.valueChanges
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe((medicineUid) => this.loadAlerts(medicineUid));
  }

  private loadAlerts(medicineUid: string | null): void {
    if (!this.patientUid || !medicineUid || medicineUid.length !== 26) {
      this.alerts.set([]);
      return;
    }
    this.prescriptionService.prescribingAlerts(this.patientUid, medicineUid).subscribe({
      next: (res) => this.alerts.set(res.alerts ?? []),
      error: () => this.alerts.set([]) // advisory only — swallow errors, never block
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    if (!this.consultationUid && !this.outsiderPatientUid) {
      this.errorMessage.set('Missing target — prescription needs a consultation or an outsider patient.');
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const payload = {
      medicineUid: raw.medicineUid,
      dose: raw.dose.trim(),
      frequency: raw.frequency.trim(),
      durationDays: raw.durationDays ?? null,
      quantity: raw.quantity ?? null,
      instructions: raw.instructions?.trim() || null
    };
    const request$ = this.consultationUid
      ? this.prescriptionService.prescribe(this.consultationUid, payload)
      : this.prescriptionService.prescribeForOutsider(this.outsiderPatientUid!, payload);
    request$.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (p) => this.activeModal.close(p),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save prescription.')
    });
  }

  medicineLabel(m: Medicine): string {
    const parts = [m.name];
    if (m.strength) parts.push(m.strength);
    if (m.form) parts.push(m.form.toLowerCase());
    return parts.join(' · ');
  }
}
