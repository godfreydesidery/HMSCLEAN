import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { MedicineService } from '../../masterdata/medicines/medicine.service';
import { Medicine } from '../../masterdata/medicines/medicine.types';
import { StockService } from './stock.service';
import { StockBatch, WASTAGE_REASONS, WastageReason } from './stock.types';

export type StockEditMode = 'receive' | 'adjust' | 'write-off';

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
  /** Adjust mode: uid of the batch being adjusted (set by the row "Adjust" action). */
  @Input() prefillBatchUid: string | null = null;
  @Input() prefillBatchLabel: string | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly stockService = inject(StockService);
  private readonly medicineService = inject(MedicineService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly medicines = signal<Medicine[]>([]);
  readonly loadingMedicines = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly wastageReasons = WASTAGE_REASONS;

  readonly form = this.fb.nonNullable.group({
    medicineUid: [''],
    batchNo: [''],
    expiresAt: [''],
    quantity: [0, [Validators.required]],
    reason: '',
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
    } else if (this.mode === 'write-off') {
      this.form.controls.reason.addValidators(Validators.required);
      this.form.controls.quantity.setValue(1);
    } else {
      this.form.controls.quantity.setValue(0);
    }
  }

  get title(): string {
    if (this.mode === 'receive') return 'Receive stock';
    if (this.mode === 'write-off') return 'Write off batch';
    return 'Adjust batch';
  }
  get quantityLabel(): string {
    if (this.mode === 'receive') return 'Quantity received';
    if (this.mode === 'write-off') return 'Quantity to write off';
    return 'Signed delta (negative removes)';
  }
  get submitLabel(): string {
    if (this.mode === 'receive') return 'Record receipt';
    if (this.mode === 'write-off') return 'Write off';
    return 'Apply adjustment';
  }

  isQuantityValid(): boolean {
    const v = this.form.controls.quantity.value;
    if (this.mode === 'adjust') return Number.isInteger(v) && v !== 0;
    return Number.isInteger(v) && v > 0;
  }

  isReasonValid(): boolean {
    if (this.mode !== 'write-off') return true;
    return !!this.form.controls.reason.value;
  }

  submit(): void {
    if (this.submitting()) return;
    this.form.markAllAsTouched();
    if (!this.isQuantityValid() || !this.isReasonValid()) return;
    const raw = this.form.getRawValue();
    this.submitting.set(true);
    this.errorMessage.set(null);
    const note = raw.note?.trim() || null;
    let obs;
    if (this.mode === 'receive') {
      obs = this.stockService.receive(this.pharmacyUid, {
        medicineUid: raw.medicineUid,
        batchNo: raw.batchNo.trim(),
        expiresAt: raw.expiresAt?.trim() || null,
        quantity: raw.quantity,
        note
      });
    } else if (this.mode === 'write-off') {
      obs = this.stockService.writeOff(this.pharmacyUid, {
        batchUid: this.prefillBatchUid!,
        quantity: raw.quantity,
        reason: raw.reason as WastageReason,
        note
      });
    } else {
      obs = this.stockService.adjust(this.pharmacyUid, {
        batchUid: this.prefillBatchUid!,
        delta: raw.quantity,
        note
      });
    }
    obs.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (batch: StockBatch) => this.activeModal.close(batch),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save.')
    });
  }
}
