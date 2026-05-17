import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { MedicineService } from '../../masterdata/medicines/medicine.service';
import { Medicine } from '../../masterdata/medicines/medicine.types';
import { StockService } from './stock.service';
import { StockBalance } from './stock.types';

export type StockEditMode = 'receive' | 'adjust';

@Component({
  selector: 'app-stock-edit',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './stock-edit.component.html'
})
export class StockEditComponent implements OnInit {
  @Input({ required: true }) pharmacyUid!: string;
  @Input({ required: true }) pharmacyName!: string;
  @Input({ required: true }) mode!: StockEditMode;
  /** When set, pre-selects the medicine and locks the picker (used by the row "Adjust" action). */
  @Input() prefillMedicineUid: string | null = null;
  @Input() prefillMedicineLabel: string | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly stockService = inject(StockService);
  private readonly medicineService = inject(MedicineService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly medicines = signal<Medicine[]>([]);
  readonly loadingMedicines = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    medicineUid: ['', [Validators.required]],
    quantity: [0, [Validators.required]],
    note: ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    if (this.prefillMedicineUid) {
      this.form.controls.medicineUid.setValue(this.prefillMedicineUid);
      this.form.controls.medicineUid.disable();
    } else {
      this.loadingMedicines.set(true);
      this.medicineService.search({ active: true, size: 300, sort: 'name,asc' })
        .pipe(finalize(() => this.loadingMedicines.set(false)))
        .subscribe({ next: (res) => this.medicines.set(res.content) });
    }
    // Adjust mode default to 0 to force the user to type a signed delta;
    // receive mode defaults to 1 to nudge toward valid input.
    this.form.controls.quantity.setValue(this.mode === 'receive' ? 1 : 0);
  }

  get title(): string { return this.mode === 'receive' ? 'Receive stock' : 'Adjust stock'; }
  get quantityLabel(): string {
    return this.mode === 'receive' ? 'Quantity received' : 'Signed delta (negative removes)';
  }
  get submitLabel(): string { return this.mode === 'receive' ? 'Record receipt' : 'Apply adjustment'; }

  isQuantityValid(): boolean {
    const v = this.form.controls.quantity.value;
    if (this.mode === 'receive') return Number.isInteger(v) && v > 0;
    return Number.isInteger(v) && v !== 0;
  }

  submit(): void {
    if (this.submitting()) return;
    this.form.markAllAsTouched();
    if (!this.isQuantityValid() || this.form.controls.medicineUid.invalid) return;
    const raw = this.form.getRawValue();
    const payload = {
      medicineUid: raw.medicineUid,
      note: raw.note?.trim() || null
    };
    this.submitting.set(true);
    this.errorMessage.set(null);
    const obs = this.mode === 'receive'
      ? this.stockService.receive(this.pharmacyUid, { ...payload, quantity: raw.quantity })
      : this.stockService.adjust(this.pharmacyUid, { ...payload, delta: raw.quantity });
    obs.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (balance: StockBalance) => this.activeModal.close(balance),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save.')
    });
  }
}
