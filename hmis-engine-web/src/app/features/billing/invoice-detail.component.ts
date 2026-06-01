import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { forkJoin } from 'rxjs';

import { EditLinePriceComponent } from './edit-line-price.component';
import { InvoiceService } from './invoice.service';
import {
  CREDIT_NOTE_REASONS, CreditNote, INVOICE_STATUSES, Invoice, InvoiceLine, InvoiceStatus,
  LINE_COVERAGE_STATUSES, LineCoverageStatus, REFUND_REASONS, Refund
} from './invoice.types';
import { IssueRefundComponent } from './issue-refund.component';
import { RaiseCreditNoteComponent } from './raise-credit-note.component';
import { RecordPaymentComponent } from './record-payment.component';

@Component({
  selector: 'app-invoice-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './invoice-detail.component.html',
  styleUrl: './invoice-detail.component.scss'
})
export class InvoiceDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly invoiceService = inject(InvoiceService);
  private readonly modal = inject(NgbModal);

  readonly statuses = INVOICE_STATUSES;
  readonly invoice = signal<Invoice | null>(null);
  readonly creditNotes = signal<CreditNote[]>([]);
  readonly refunds = signal<Refund[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly canIssue = computed(() => this.invoice()?.status === 'DRAFT');
  readonly canPay = computed(() => {
    const s = this.invoice()?.status;
    return s === 'ISSUED' || s === 'PARTIALLY_PAID';
  });
  readonly canCancel = computed(() => {
    const s = this.invoice()?.status;
    return s !== 'PAID' && s !== 'CANCELLED';
  });
  /** A write-down is allowed once issued (not DRAFT/CANCELLED) while a balance remains. */
  readonly canCredit = computed(() => {
    const i = this.invoice();
    return !!i && i.status !== 'DRAFT' && i.status !== 'CANCELLED' && i.balance > 0;
  });
  /** A refund is allowed once cash has been received (not DRAFT/CANCELLED). */
  readonly canRefund = computed(() => {
    const i = this.invoice();
    return !!i && i.status !== 'DRAFT' && i.status !== 'CANCELLED' && i.totalPaid > 0;
  });

  constructor() {
    const uid = this.route.snapshot.paramMap.get('uid');
    if (!uid) { this.loading.set(false); this.errorMessage.set('Missing invoice identifier.'); return; }
    this.reload(uid);
  }

  private reload(uid: string): void {
    this.loading.set(true);
    forkJoin({
      invoice: this.invoiceService.findByUid(uid),
      creditNotes: this.invoiceService.listCreditNotes(uid),
      refunds: this.invoiceService.listRefunds(uid)
    }).subscribe({
      next: ({ invoice, creditNotes, refunds }) => {
        this.invoice.set(invoice); this.creditNotes.set(creditNotes); this.refunds.set(refunds);
        this.loading.set(false);
      },
      error: (err) => { this.errorMessage.set(err?.error?.message ?? 'Could not load invoice.'); this.loading.set(false); }
    });
  }

  back(): void { void this.router.navigate(['/billing']); }

  issue(): void {
    const i = this.invoice(); if (!i) return;
    this.invoiceService.issue(i.uid).subscribe({
      next: (updated) => this.invoice.set(updated),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not issue invoice.')
    });
  }

  cancel(): void {
    const i = this.invoice(); if (!i) return;
    const reason = globalThis.prompt('Reason for cancelling this invoice?')?.trim() ?? null;
    this.invoiceService.cancel(i.uid, reason).subscribe({
      next: (updated) => this.invoice.set(updated),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not cancel invoice.')
    });
  }

  recordPayment(): void {
    const i = this.invoice(); if (!i) return;
    const ref = this.modal.open(RecordPaymentComponent, { size: 'lg', backdrop: 'static' });
    (ref.componentInstance as RecordPaymentComponent).invoice = i;
    ref.closed.subscribe((updated: Invoice | undefined) => {
      if (updated) this.invoice.set(updated);
    });
  }

  raiseCreditNote(): void {
    const i = this.invoice(); if (!i) return;
    const ref = this.modal.open(RaiseCreditNoteComponent, { size: 'lg', backdrop: 'static' });
    (ref.componentInstance as RaiseCreditNoteComponent).invoice = i;
    // The POST returns the credit note, not the invoice — reload to refresh balance/status + the list.
    ref.closed.subscribe((cn: CreditNote | undefined) => { if (cn) this.reload(i.uid); });
  }

  issueRefund(): void {
    const i = this.invoice(); if (!i) return;
    const ref = this.modal.open(IssueRefundComponent, { size: 'lg', backdrop: 'static' });
    (ref.componentInstance as IssueRefundComponent).invoice = i;
    ref.closed.subscribe((r: Refund | undefined) => { if (r) this.reload(i.uid); });
  }

  editLinePrice(line: InvoiceLine): void {
    const i = this.invoice(); if (!i) return;
    const ref = this.modal.open(EditLinePriceComponent, { backdrop: 'static' });
    const inst = ref.componentInstance as EditLinePriceComponent;
    inst.invoice = i; inst.line = line;
    // Override returns the recomputed invoice (new subtotal/balance/band).
    ref.closed.subscribe((updated: Invoice | undefined) => { if (updated) this.invoice.set(updated); });
  }

  creditReasonLabel(r: string): string { return CREDIT_NOTE_REASONS.find((x) => x.value === r)?.label ?? r; }
  refundReasonLabel(r: string): string { return REFUND_REASONS.find((x) => x.value === r)?.label ?? r; }

  statusBadgeClass(s: InvoiceStatus): string {
    return 'badge ' + (this.statuses.find((x) => x.value === s)?.badgeClass ?? '');
  }
  statusLabel(s: InvoiceStatus): string {
    return this.statuses.find((x) => x.value === s)?.label ?? s;
  }

  coverageBadgeClass(s: LineCoverageStatus): string {
    return LINE_COVERAGE_STATUSES.find((x) => x.value === s)?.badgeClass ?? 'text-bg-light border';
  }
  coverageLabel(s: LineCoverageStatus): string {
    return LINE_COVERAGE_STATUSES.find((x) => x.value === s)?.label ?? s;
  }
}
