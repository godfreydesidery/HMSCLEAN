import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { PurchaseOrder } from '../order/purchase-order.types';
import { SupplierInvoiceService } from './supplier-invoice.service';
import { SupplierInvoice } from './supplier-invoice.types';

interface LineMeta { label: string; ordered: number; received: number; }

/**
 * Create a DRAFT supplier invoice against a received purchase order. Only PO lines
 * that have been received (received > 0) are billable; each line defaults to the
 * received quantity and the PO unit cost. The three-way match is enforced at approve.
 */
@Component({
  selector: 'app-supplier-invoice-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './supplier-invoice-create.component.html'
})
export class SupplierInvoiceCreateComponent implements OnInit {
  @Input({ required: true }) order!: PurchaseOrder;

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(SupplierInvoiceService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly meta: LineMeta[] = [];

  readonly form = this.fb.group({
    supplierInvoiceNo: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(64)]),
    invoiceDate: this.fb.nonNullable.control(this.today(), [Validators.required]),
    dueDate: this.fb.nonNullable.control(''),
    notes: this.fb.nonNullable.control('', [Validators.maxLength(500)]),
    lines: this.fb.array<FormGroup>([])
  });

  get lines(): FormArray<FormGroup> { return this.form.controls.lines; }

  ngOnInit(): void {
    for (const l of this.order.lines) {
      if (l.receivedQuantity <= 0) continue;
      this.meta.push({ label: `${l.medicineCode ?? ''} ${l.medicineName ?? l.medicineUid}`.trim(), ordered: l.orderedQuantity, received: l.receivedQuantity });
      this.lines.push(this.fb.group({
        include: this.fb.nonNullable.control(true),
        poLineUid: this.fb.nonNullable.control(l.uid),
        invoicedQuantity: this.fb.nonNullable.control(l.receivedQuantity, [Validators.required, Validators.min(1), Validators.max(l.receivedQuantity)]),
        unitCost: this.fb.nonNullable.control(l.unitCost, [Validators.required, Validators.min(0)])
      }));
    }
  }

  private today(): string { return new Date().toISOString().slice(0, 10); }

  lineTotal(i: number): number {
    const g = this.lines.at(i);
    return Number(g.controls['invoicedQuantity'].value) * Number(g.controls['unitCost'].value);
  }
  get grandTotal(): number {
    let t = 0;
    this.lines.controls.forEach((g, i) => { if (g.controls['include'].value) t += this.lineTotal(i); });
    return t;
  }

  submit(): void {
    if (this.submitting()) return;
    const selected = this.lines.controls.filter((g) => g.controls['include'].value);
    if (this.form.invalid || selected.length === 0) {
      this.form.markAllAsTouched();
      if (selected.length === 0) this.errorMessage.set('Select at least one line to invoice.');
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.service.create({
      orderUid: this.order.uid,
      supplierInvoiceNo: raw.supplierInvoiceNo.trim(),
      invoiceDate: raw.invoiceDate,
      dueDate: raw.dueDate || null,
      currency: this.order.currency || null,
      notes: raw.notes?.trim() || null,
      lines: selected.map((g) => ({
        poLineUid: g.controls['poLineUid'].value as string,
        invoicedQuantity: Number(g.controls['invoicedQuantity'].value),
        unitCost: Number(g.controls['unitCost'].value)
      }))
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (inv: SupplierInvoice) => this.activeModal.close(inv),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not create the supplier invoice.')
    });
  }
}
