import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { InvoiceService } from './invoice.service';
import { INVOICE_STATUSES, Invoice, InvoiceStatus, LINE_COVERAGE_STATUSES, LineCoverageStatus } from './invoice.types';
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

  constructor() {
    const uid = this.route.snapshot.paramMap.get('uid');
    if (!uid) { this.loading.set(false); this.errorMessage.set('Missing invoice identifier.'); return; }
    this.invoiceService.findByUid(uid).subscribe({
      next: (i) => { this.invoice.set(i); this.loading.set(false); },
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
