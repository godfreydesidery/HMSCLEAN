import { CommonModule } from '@angular/common';
import { Component, Input, inject } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { PrintLayoutComponent, PrintMeta } from '../../shared/print/print-layout.component';

export interface ReceiptLine {
  description: string;
  kind: string;
  amount: number;
}

/**
 * Data for a cashier payment receipt (BILL-1) — a flattened view of a
 * line-level collection (PayLinesResult) plus the patient display fields.
 */
export interface ReceiptDocumentData {
  patientName: string | null;
  patientNo: string | null;
  receiptNos: string[];
  method: string | null;
  currency: string;
  totalCollected: number;
  lines: ReceiptLine[];
  paidAt: string;
}

/**
 * Printable POS receipt (BILL-1) for a cashier collection. Opened in an NgbModal
 * from the cashier after a successful "collect", rendered through the shared
 * {@link PrintLayoutComponent} letterhead.
 */
@Component({
  selector: 'app-receipt-document',
  standalone: true,
  imports: [CommonModule, PrintLayoutComponent],
  templateUrl: './receipt-document.component.html'
})
export class ReceiptDocumentComponent {
  @Input({ required: true }) data!: ReceiptDocumentData;

  protected readonly activeModal = inject(NgbActiveModal);

  get documentNo(): string | null {
    return this.data.receiptNos.length ? this.data.receiptNos.join(', ') : null;
  }

  get meta(): PrintMeta[] {
    const d = this.data;
    return [
      { label: 'Patient', value: d.patientName },
      { label: 'Patient No', value: d.patientNo },
      { label: 'Method', value: d.method }
    ];
  }
}
