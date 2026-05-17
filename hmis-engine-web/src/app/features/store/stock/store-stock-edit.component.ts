import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { MedicineService } from '../../masterdata/medicines/medicine.service';
import { Medicine } from '../../masterdata/medicines/medicine.types';
import { StoreStockService } from './store-stock.service';
import { StoreStockBatch } from './store-stock.types';

export type StoreStockEditMode = 'receive' | 'adjust';

@Component({
  selector: 'app-store-stock-edit',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './store-stock-edit.component.html'
})
export class StoreStockEditComponent implements OnInit {
  @Input({ required: true }) storeUid!: string;
  @Input({ required: true }) storeName!: string;
  @Input({ required: true }) mode!: StoreStockEditMode;
  @Input() prefillBatchUid: string | null = null;
  @Input() prefillBatchLabel: string | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly stockService = inject(StoreStockService);
  private readonly medicineService = inject(MedicineService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly medicines = signal<Medicine[]>([]);
  readonly loadingMedicines = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    medicineUid: [''],
    batchNo: [''],
    expiresAt: [''],
    quantity: [0, [Validators.required]],
    note: ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    if (this.mode === 'receive') {
      this.form.controls.medicineUid.addValidators(Validators.required);
      this.form.controls.batchNo.addValidators([Validators.required, Validators.maxLength(64)]);
      this.loadingMedicines.set(true);
      this.medicineService.search({ active: true, size: 300, sort: 'name,asc' })
        .pipe(finalize(() => this.loadingMedicines.set(false)))
        .subscribe({ next: (res) => this.medicines.set(res.content) });
      this.form.controls.quantity.setValue(1);
    } else {
      this.form.controls.quantity.setValue(0);
    }
  }

  get title(): string { return this.mode === 'receive' ? 'Receive stock' : 'Adjust batch'; }
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
    if (!this.isQuantityValid()) return;
    const raw = this.form.getRawValue();
    this.submitting.set(true);
    this.errorMessage.set(null);
    const note = raw.note?.trim() || null;
    const obs = this.mode === 'receive'
      ? this.stockService.receive(this.storeUid, {
          medicineUid: raw.medicineUid,
          batchNo: raw.batchNo.trim(),
          expiresAt: raw.expiresAt?.trim() || null,
          quantity: raw.quantity,
          note
        })
      : this.stockService.adjust(this.storeUid, {
          batchUid: this.prefillBatchUid!,
          delta: raw.quantity,
          note
        });
    obs.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (batch: StoreStockBatch) => this.activeModal.close(batch),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save.')
    });
  }
}
