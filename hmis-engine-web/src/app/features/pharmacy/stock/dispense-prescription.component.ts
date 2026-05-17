import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { Prescription } from '../../encounter/prescription/prescription.types';
import { PharmacyService } from '../../masterdata/pharmacies/pharmacy.service';
import { Pharmacy } from '../../masterdata/pharmacies/pharmacy.types';
import { StockService } from './stock.service';
import { StockMovement } from './stock.types';

@Component({
  selector: 'app-dispense-prescription',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './dispense-prescription.component.html'
})
export class DispensePrescriptionComponent implements OnInit {
  @Input({ required: true }) prescription!: Prescription;

  private readonly fb = inject(FormBuilder);
  private readonly stockService = inject(StockService);
  private readonly pharmacyService = inject(PharmacyService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly pharmacies = signal<Pharmacy[]>([]);
  readonly loadingPharmacies = signal(true);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    pharmacyUid: ['', [Validators.required]]
  });

  ngOnInit(): void {
    this.pharmacyService.search({ active: true, size: 200, sort: 'name,asc' })
      .pipe(finalize(() => this.loadingPharmacies.set(false)))
      .subscribe({
        next: (page) => {
          this.pharmacies.set(page.content);
          if (page.content.length === 1) {
            this.form.controls.pharmacyUid.setValue(page.content[0].uid);
          }
        },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load pharmacies.')
      });
  }

  dispense(): void {
    if (this.submitting()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.stockService.dispense(raw.pharmacyUid, this.prescription.uid)
      .pipe(finalize(() => this.submitting.set(false))).subscribe({
        next: (movement: StockMovement) => this.activeModal.close(movement),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not dispense.')
      });
  }
}
