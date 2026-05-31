import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { MedicineService } from '../../masterdata/medicines/medicine.service';
import { Medicine } from '../../masterdata/medicines/medicine.types';
import { SupplierItemPriceService } from './supplier-item-price.service';
import { SupplierItemPrice } from './supplier-item-price.types';

/** Create / edit one supplier item-price quote. */
@Component({
  selector: 'app-supplier-price-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="modal-header">
      <h5 class="modal-title">{{ existing ? 'Edit quote' : 'Add quote' }}</h5>
      <button type="button" class="btn-close" (click)="activeModal.dismiss()" aria-label="Close"></button>
    </div>
    <div class="modal-body">
      @if (errorMessage()) {
        <div class="alert alert-danger d-flex align-items-center gap-2"><i class="bi bi-exclamation-circle"></i><span>{{ errorMessage() }}</span></div>
      }
      <form [formGroup]="form" class="row g-3">
        <div class="col-12">
          <label class="form-label" for="sp-med">Medicine <span class="text-danger">*</span></label>
          @if (existing) {
            <input id="sp-med" type="text" class="form-control" [value]="(existing.medicineCode ?? '') + ' — ' + (existing.medicineName ?? '')" disabled>
          } @else {
            <select id="sp-med" class="form-select" formControlName="medicineUid">
              <option value="" disabled>{{ loadingMedicines() ? 'Loading…' : '— Select medicine —' }}</option>
              @for (m of medicines(); track m.uid) {
                <option [value]="m.uid">{{ m.code }} — {{ m.name }}@if (m.strength) { <span> · {{ m.strength }}</span> }</option>
              }
            </select>
          }
        </div>
        <div class="col-md-8">
          <label class="form-label" for="sp-price">Unit price <span class="text-danger">*</span></label>
          <input id="sp-price" type="number" min="0.01" step="0.01" class="form-control" formControlName="unitPrice">
        </div>
        <div class="col-md-4">
          <label class="form-label" for="sp-cur">Currency</label>
          <input id="sp-cur" type="text" maxlength="3" class="form-control text-uppercase" formControlName="currency">
        </div>
        @if (!existing) {
          <div class="col-md-6">
            <label class="form-label" for="sp-from">Valid from <span class="text-danger">*</span></label>
            <input id="sp-from" type="date" class="form-control" formControlName="validFrom">
          </div>
        }
        <div class="col-md-6">
          <label class="form-label" for="sp-to">Valid to <span class="hmis-muted small">(open-ended if blank)</span></label>
          <input id="sp-to" type="date" class="form-control" formControlName="validTo">
        </div>
        <div class="col-12">
          <label class="form-label" for="sp-notes">Notes</label>
          <textarea id="sp-notes" rows="2" class="form-control" formControlName="notes" maxlength="500"></textarea>
        </div>
      </form>
    </div>
    <div class="modal-footer">
      <button type="button" class="btn btn-light border" (click)="activeModal.dismiss()" [disabled]="submitting()">Cancel</button>
      <button type="button" class="btn btn-primary d-flex align-items-center gap-2" (click)="submit()" [disabled]="submitting() || form.invalid">
        @if (submitting()) { <output class="spinner-border spinner-border-sm" aria-live="polite"><span class="visually-hidden">Saving</span></output> }
        <i class="bi bi-check2"></i>{{ existing ? 'Save' : 'Add' }}
      </button>
    </div>
  `
})
export class SupplierPriceFormComponent implements OnInit {
  @Input({ required: true }) supplierUid!: string;
  @Input() existing: SupplierItemPrice | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly priceService = inject(SupplierItemPriceService);
  private readonly medicineService = inject(MedicineService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly medicines = signal<Medicine[]>([]);
  readonly loadingMedicines = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    medicineUid: ['', [Validators.required]],
    unitPrice: [0, [Validators.required, Validators.min(0.01)]],
    currency: ['TZS', [Validators.required, Validators.minLength(3), Validators.maxLength(3)]],
    validFrom: ['', [Validators.required]],
    validTo: [''],
    notes: ['']
  });

  ngOnInit(): void {
    if (this.existing) {
      this.form.patchValue({
        medicineUid: this.existing.medicineUid,
        unitPrice: this.existing.unitPrice,
        currency: this.existing.currency,
        validFrom: this.existing.validFrom,
        validTo: this.existing.validTo ?? '',
        notes: this.existing.notes ?? ''
      });
      this.form.controls.medicineUid.clearValidators();
      this.form.controls.medicineUid.updateValueAndValidity();
      this.form.controls.validFrom.clearValidators();
      this.form.controls.validFrom.updateValueAndValidity();
    } else {
      this.loadingMedicines.set(true);
      this.medicineService.search({ active: true, size: 300, sort: 'name,asc' })
        .pipe(finalize(() => this.loadingMedicines.set(false)))
        .subscribe({ next: (page) => this.medicines.set(page.content) });
    }
  }

  submit(): void {
    if (this.submitting()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const currency = raw.currency.trim().toUpperCase();
    const obs = this.existing
      ? this.priceService.update(this.supplierUid, this.existing.uid, {
          unitPrice: raw.unitPrice,
          currency,
          validTo: raw.validTo || null,
          notes: raw.notes.trim() || null
        })
      : this.priceService.create(this.supplierUid, {
          medicineUid: raw.medicineUid,
          unitPrice: raw.unitPrice,
          currency,
          validFrom: raw.validFrom,
          validTo: raw.validTo || null,
          notes: raw.notes.trim() || null
        });
    obs.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (saved) => this.activeModal.close(saved),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save the quote.')
    });
  }
}
