import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { MedicineService } from '../../masterdata/medicines/medicine.service';
import { Medicine } from '../../masterdata/medicines/medicine.types';
import { PurchaseOrderService } from './purchase-order.service';
import { PurchaseOrder, PurchaseOrderLine } from './purchase-order.types';

@Component({
  selector: 'app-po-add-line',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './add-line.component.html'
})
export class AddLineComponent implements OnInit {
  @Input({ required: true }) orderUid!: string;
  @Input() existing: PurchaseOrderLine | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly purchaseOrderService = inject(PurchaseOrderService);
  private readonly medicineService = inject(MedicineService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly medicines = signal<Medicine[]>([]);
  readonly loadingMedicines = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    medicineUid: ['', [Validators.required]],
    orderedQuantity: [1, [Validators.required, Validators.min(1)]],
    unitCost: [0, [Validators.required, Validators.min(0)]],
    currency: ['TZS', [Validators.maxLength(3)]]
  });

  get isEdit(): boolean { return this.existing != null; }
  get title(): string { return this.isEdit ? 'Edit line' : 'Add line'; }

  ngOnInit(): void {
    if (this.existing) {
      this.form.patchValue({
        medicineUid: this.existing.medicineUid,
        orderedQuantity: this.existing.orderedQuantity,
        unitCost: this.existing.unitCost,
        currency: this.existing.currency
      });
      this.form.controls.medicineUid.disable();
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
    const obs = this.isEdit
      ? this.purchaseOrderService.updateLine(this.orderUid, this.existing!.uid, {
          orderedQuantity: raw.orderedQuantity,
          unitCost: raw.unitCost,
          currency: raw.currency || null
        })
      : this.purchaseOrderService.addLine(this.orderUid, {
          medicineUid: raw.medicineUid,
          orderedQuantity: raw.orderedQuantity,
          unitCost: raw.unitCost,
          currency: raw.currency || null
        });
    obs.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (po: PurchaseOrder) => this.activeModal.close(po),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save line.')
    });
  }
}
