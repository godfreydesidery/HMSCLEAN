import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { PharmacyService } from '../../masterdata/pharmacies/pharmacy.service';
import { Pharmacy } from '../../masterdata/pharmacies/pharmacy.types';

/**
 * Collects the optional Phase 37 sales-pharmacy override for a dispense.
 * Closes with the chosen pharmacy uid, or `null` to keep the default
 * (the pharmacy that opened the sale). It does not dispense itself — the
 * caller owns the service call so busy/refresh state stays in one place.
 */
@Component({
  selector: 'app-dispense-line-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './dispense-line-modal.component.html'
})
export class DispenseLineModalComponent implements OnInit {
  @Input({ required: true }) openedAtPharmacyUid!: string;
  @Input() openedAtPharmacyName: string | null = null;
  @Input() medicineLabel: string | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly pharmacyService = inject(PharmacyService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly pharmacies = signal<Pharmacy[]>([]);

  readonly form = this.fb.nonNullable.group({
    /** '' = no override (dispense from the sale's own pharmacy). */
    salesPharmacyUid: ['']
  });

  ngOnInit(): void {
    this.pharmacyService.search({ active: true, size: 200, sort: 'name,asc' })
      .subscribe({ next: (r) => this.pharmacies.set(r.content), error: () => { /* dropdown stays empty; default still works */ } });
  }

  confirm(): void {
    const override = this.form.controls.salesPharmacyUid.value.trim();
    this.activeModal.close(override && override !== this.openedAtPharmacyUid ? override : null);
  }
}
