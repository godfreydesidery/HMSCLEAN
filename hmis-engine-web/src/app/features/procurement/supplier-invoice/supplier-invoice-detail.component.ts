import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NgbDropdownModule, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { SupplierInvoicePayComponent } from './supplier-invoice-pay.component';
import { SupplierInvoiceService } from './supplier-invoice.service';
import { SUPPLIER_INVOICE_STATUSES, SupplierInvoice, SupplierInvoiceStatus } from './supplier-invoice.types';

@Component({
  selector: 'app-supplier-invoice-detail',
  standalone: true,
  imports: [CommonModule, NgbDropdownModule],
  templateUrl: './supplier-invoice-detail.component.html'
})
export class SupplierInvoiceDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(SupplierInvoiceService);
  private readonly modal = inject(NgbModal);

  readonly statuses = SUPPLIER_INVOICE_STATUSES;
  readonly invoice = signal<SupplierInvoice | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly actionMessage = signal<string | null>(null);

  readonly canSubmit = computed(() => this.invoice()?.status === 'DRAFT' && (this.invoice()?.lines.length ?? 0) > 0);
  readonly canApprove = computed(() => this.invoice()?.status === 'SUBMITTED');
  readonly canPay = computed(() => this.invoice()?.status === 'APPROVED');
  readonly canReject = computed(() => this.invoice()?.status === 'SUBMITTED');
  readonly canCancel = computed(() => this.invoice()?.status === 'DRAFT');

  constructor() {
    const uid = this.route.snapshot.paramMap.get('uid');
    if (!uid) { this.loading.set(false); this.errorMessage.set('Missing invoice identifier.'); return; }
    this.load(uid);
  }

  private load(uid: string): void {
    this.loading.set(true);
    this.service.findByUid(uid).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (inv) => this.invoice.set(inv),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load supplier invoice.')
    });
  }

  back(): void { void this.router.navigate(['/procurement/supplier-invoices']); }

  submit(): void { const i = this.invoice(); if (i) this.run(this.service.submit(i.uid), 'Submitted.'); }
  approve(): void {
    const i = this.invoice(); if (!i) return;
    if (!globalThis.confirm('Approve this invoice? The three-way match (invoiced ≤ received) is enforced now.')) return;
    this.run(this.service.approve(i.uid), 'Approved — three-way match passed.');
  }
  reject(): void {
    const i = this.invoice(); if (!i) return;
    const reason = globalThis.prompt('Reason for rejecting this invoice?')?.trim() ?? null;
    if (reason === null) return;
    this.run(this.service.reject(i.uid, reason || null), 'Rejected.');
  }
  cancel(): void {
    const i = this.invoice(); if (!i) return;
    if (!globalThis.confirm('Cancel this draft invoice?')) return;
    this.run(this.service.cancel(i.uid), 'Cancelled.');
  }

  pay(): void {
    const i = this.invoice(); if (!i) return;
    const ref = this.modal.open(SupplierInvoicePayComponent, { backdrop: 'static' });
    (ref.componentInstance as SupplierInvoicePayComponent).invoice = i;
    ref.closed.subscribe((updated: SupplierInvoice | undefined) => {
      if (updated) { this.invoice.set(updated); this.actionMessage.set('Marked paid.'); }
    });
  }

  private run(req$: ReturnType<SupplierInvoiceService['submit']>, msg: string): void {
    this.busy.set(true);
    this.errorMessage.set(null);
    req$.pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (inv) => { this.invoice.set(inv); this.actionMessage.set(msg); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'The action failed.')
    });
  }

  statusBadgeClass(s: SupplierInvoiceStatus): string { return 'badge ' + (this.statuses.find((x) => x.value === s)?.badgeClass ?? ''); }
  statusLabel(s: SupplierInvoiceStatus): string { return this.statuses.find((x) => x.value === s)?.label ?? s; }
}
