import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

import { environment } from '../../../environments/environment';

export interface PrintMeta {
  label: string;
  value: string | null | undefined;
}

/**
 * Reusable letterhead "sheet" for any printable document (receipts, closure
 * documents, invoices). The host opens a document component in an NgbModal; that
 * component composes this layout and projects the body. The on-screen toolbar is
 * marked {@code .no-print}; the sheet itself is {@code .hmis-print-root}, which
 * the global {@code @media print} rule reveals while hiding the rest of the app —
 * so "Print" produces just this document (or "Save as PDF" from the browser).
 */
@Component({
  selector: 'app-print-layout',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './print-layout.component.html'
})
export class PrintLayoutComponent {
  /** Document heading, e.g. "Discharge Summary" / "Payment Receipt" / "Invoice". */
  @Input() title = '';
  /** Optional sub-heading under the title (e.g. the patient class or scope). */
  @Input() subtitle: string | null = null;
  /** Optional document identifier shown top-right (receipt no, invoice no, plan uid). */
  @Input() documentNo: string | null = null;
  /** Key/value rows rendered in the header meta grid (patient, date, clinician…). */
  @Input() meta: PrintMeta[] = [];
  /** Optional small print at the foot of the page. */
  @Input() footerNote: string | null = null;

  @Output() readonly closed = new EventEmitter<void>();

  readonly facilityName = environment.appName;
  readonly printedAt = new Date();

  doPrint(): void {
    window.print();
  }

  close(): void {
    this.closed.emit();
  }
}
