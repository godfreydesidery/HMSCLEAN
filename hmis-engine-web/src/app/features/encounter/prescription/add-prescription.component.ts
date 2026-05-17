import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { MedicineService } from '../../masterdata/medicines/medicine.service';
import { Medicine } from '../../masterdata/medicines/medicine.types';
import { PrescriptionService } from './prescription.service';

@Component({
  selector: 'app-add-prescription',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './add-prescription.component.html'
})
export class AddPrescriptionComponent implements OnInit {
  @Input({ required: true }) consultationUid!: string;

  private readonly fb = inject(FormBuilder);
  private readonly prescriptionService = inject(PrescriptionService);
  private readonly medicineService = inject(MedicineService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly medicines = signal<Medicine[]>([]);
  readonly loadingMedicines = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

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
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.prescriptionService.prescribe(this.consultationUid, {
      medicineUid: raw.medicineUid,
      dose: raw.dose.trim(),
      frequency: raw.frequency.trim(),
      durationDays: raw.durationDays ?? null,
      quantity: raw.quantity ?? null,
      instructions: raw.instructions?.trim() || null
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
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
