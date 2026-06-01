import { CommonModule } from '@angular/common';
import { Component, Input, inject } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { PrintLayoutComponent, PrintMeta } from '../../shared/print/print-layout.component';
import { Invoice } from './invoice.types';

/**
 * Printable patient invoice (BILL-4). Opened in an NgbModal from invoice-detail;
 * renders the invoice lines, totals and payments through the shared
 * {@link PrintLayoutComponent} letterhead.
 */
@Component({
  selector: 'app-invoice-document',
  standalone: true,
  imports: [CommonModule, PrintLayoutComponent],
  templateUrl: './invoice-document.component.html'
})
export class InvoiceDocumentComponent {
  @Input({ required: true }) invoice!: Invoice;

  protected readonly activeModal = inject(NgbActiveModal);

  get meta(): PrintMeta[] {
    const i = this.invoice;
    return [
      { label: 'Patient', value: i.patientName },
      { label: 'Patient No', value: i.patientNo },
      { label: 'Status', value: i.status },
      { label: 'Payment type', value: i.paymentType },
      { label: 'Insurance', value: i.insurancePlanName },
      { label: 'Issued', value: i.issuedAt ? new Date(i.issuedAt).toLocaleString() : null }
    ];
  }
}
