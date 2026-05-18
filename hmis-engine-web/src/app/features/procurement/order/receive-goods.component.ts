import { CommonModule } from '@angular/common';
import { Component, Input, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { PurchaseOrderService } from './purchase-order.service';
import { GoodsReceipt, PurchaseOrder, PurchaseOrderLine } from './purchase-order.types';

@Component({
  selector: 'app-receive-goods',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './receive-goods.component.html'
})
export class ReceiveGoodsComponent {
  @Input({ required: true }) set order(po: PurchaseOrder) {
    this.po.set(po);
    this.buildLineForms(po.lines);
  }

  private readonly fb = inject(FormBuilder);
  private readonly purchaseOrderService = inject(PurchaseOrderService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly po = signal<PurchaseOrder | null>(null);
  readonly lineForms = signal<FormGroup[]>([]);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    deliveryNote: ['', [Validators.maxLength(120)]],
    notes: ['', [Validators.maxLength(500)]]
  });

  private buildLineForms(lines: PurchaseOrderLine[]): void {
    const forms = lines
      .filter((l) => l.outstandingQuantity > 0)
      .map((line) => this.fb.nonNullable.group({
        poLineUid: [line.uid],
        medicineLabel: [(line.medicineCode ?? '') + ' — ' + (line.medicineName ?? '')],
        outstanding: [line.outstandingQuantity],
        quantity: [line.outstandingQuantity, [Validators.min(0), Validators.max(line.outstandingQuantity)]],
        batchNo: ['', [Validators.required, Validators.maxLength(64)]],
        expiresAt: ['']
      }));
    this.lineForms.set(forms);
  }

  submit(): void {
    if (this.submitting()) return;
    const lines = this.lineForms()
      .map((fg) => ({
        poLineUid: fg.controls['poLineUid'].value as string,
        quantity: Number(fg.controls['quantity'].value) || 0,
        batchNo: ((fg.controls['batchNo'].value as string) || '').trim(),
        expiresAt: ((fg.controls['expiresAt'].value as string) || '').trim() || null
      }))
      .filter((l) => l.quantity > 0);
    if (lines.length === 0) {
      this.errorMessage.set('Enter at least one line quantity greater than zero.');
      return;
    }
    const missingBatch = lines.find((l) => !l.batchNo);
    if (missingBatch) {
      this.errorMessage.set('Every received line needs a batch number from the supplier.');
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const po = this.po();
    if (!po) return;
    this.purchaseOrderService.recordReceipt(po.uid, {
      deliveryNote: raw.deliveryNote?.trim() || null,
      notes: raw.notes?.trim() || null,
      lines
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (receipt: GoodsReceipt) => this.activeModal.close(receipt),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not record receipt.')
    });
  }
}
