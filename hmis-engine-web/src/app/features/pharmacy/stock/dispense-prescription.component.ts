import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { WorkingLocationService, WorkingLocationRef } from '../../../core/working-location/working-location.service';
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
  private readonly workingLocation = inject(WorkingLocationService);
  private readonly router = inject(Router);
  protected readonly activeModal = inject(NgbActiveModal);

  /** The filling pharmacy = the operator's own working pharmacy (read-only, legacy). */
  readonly workingPharmacy = signal<WorkingLocationRef | null>(null);
  /** Other active pharmacies the dispense can be SOURCED from (Phase 37 counterparty override). */
  readonly pharmacies = signal<Pharmacy[]>([]);
  readonly loadingPharmacies = signal(true);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    /** Phase 37 multi-pharmacy: when set + different from the working pharmacy, stock comes from here. */
    salesPharmacyUid: ['']
  });

  ngOnInit(): void {
    // The filling pharmacy is the working pharmacy only — no own-pharmacy picker.
    const working = this.workingLocation.workingPharmacy();
    this.workingPharmacy.set(working);
    if (!working) {
      this.loadingPharmacies.set(false);
      this.errorMessage.set('No working pharmacy is selected. Choose one from "Select pharmacy" before dispensing.');
      return;
    }
    // Load the other pharmacies purely to populate the optional sales-source override.
    this.pharmacyService.search({ active: true, size: 200, sort: 'name,asc' })
      .pipe(finalize(() => this.loadingPharmacies.set(false)))
      .subscribe({
        next: (page) => this.pharmacies.set(page.content),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load pharmacies.')
      });
  }

  /** Dismiss the modal and head to the pharmacy picker to switch working pharmacy. */
  changePharmacy(): void {
    this.activeModal.dismiss();
    void this.router.navigate(['/pharmacy/select']);
  }

  dispense(): void {
    if (this.submitting()) return;
    const working = this.workingPharmacy();
    if (!working) return;
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.stockService.dispense(working.uid, this.prescription.uid, raw.salesPharmacyUid || null)
      .pipe(finalize(() => this.submitting.set(false))).subscribe({
        next: (movements: StockMovement[]) => this.activeModal.close(movements),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not dispense.')
      });
  }
}
