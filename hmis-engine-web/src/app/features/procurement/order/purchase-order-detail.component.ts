import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NgbDropdownModule, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize, forkJoin } from 'rxjs';

import { AuthService } from '../../../core/auth/auth.service';
import { SupplierInvoiceCreateComponent } from '../supplier-invoice/supplier-invoice-create.component';
import { SupplierInvoice } from '../supplier-invoice/supplier-invoice.types';
import { AddLineComponent } from './add-line.component';
import { PurchaseOrderService } from './purchase-order.service';
import {
  GOODS_RECEIPT_STATUSES, GoodsReceipt, GoodsReceiptStatus,
  PURCHASE_ORDER_STATUSES, PurchaseOrder, PurchaseOrderLine, PurchaseOrderStatus
} from './purchase-order.types';
import { ReceiveGoodsComponent } from './receive-goods.component';

@Component({
  selector: 'app-purchase-order-detail',
  standalone: true,
  imports: [CommonModule, NgbDropdownModule],
  templateUrl: './purchase-order-detail.component.html'
})
export class PurchaseOrderDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly purchaseOrderService = inject(PurchaseOrderService);
  private readonly modal = inject(NgbModal);
  private readonly auth = inject(AuthService);

  readonly statuses = PURCHASE_ORDER_STATUSES;
  readonly grnStatuses = GOODS_RECEIPT_STATUSES;
  readonly order = signal<PurchaseOrder | null>(null);
  readonly receipts = signal<GoodsReceipt[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly actionMessage = signal<string | null>(null);

  readonly canEdit = computed(() => this.order()?.status === 'DRAFT');
  // Approval chain (mirrors the backend guards + segregation-of-duties authorities):
  // DRAFT --verify--> VERIFIED --approve--> APPROVED --submit(order)--> ORDERED.
  readonly canVerify = computed(() => {
    const o = this.order();
    return o?.status === 'DRAFT' && o.lines.length > 0 && this.auth.hasPrivilege('PROCUREMENT_VERIFY');
  });
  readonly canApprove = computed(() =>
    this.order()?.status === 'VERIFIED' && this.auth.hasPrivilege('PROCUREMENT_APPROVE'));
  readonly canOrder = computed(() => this.order()?.status === 'APPROVED');
  readonly canReject = computed(() => {
    const s = this.order()?.status;
    return s === 'DRAFT' || s === 'VERIFIED' || s === 'APPROVED';
  });
  readonly canReceive = computed(() => {
    const s = this.order()?.status;
    return s === 'ORDERED' || s === 'PARTIALLY_RECEIVED';
  });
  readonly canCancel = computed(() => {
    const s = this.order()?.status;
    return s != null && s !== 'RECEIVED' && s !== 'CANCELLED' && s !== 'REJECTED';
  });
  readonly canVerifyGrn = computed(() => this.auth.hasPrivilege('PROCUREMENT_VERIFY'));
  readonly canApproveGrn = computed(() => this.auth.hasPrivilege('PROCUREMENT_APPROVE'));
  // Once goods have been received against the PO, the supplier's bill can be raised.
  readonly canCreateInvoice = computed(() => {
    const s = this.order()?.status;
    return s === 'PARTIALLY_RECEIVED' || s === 'RECEIVED';
  });

  constructor() {
    const uid = this.route.snapshot.paramMap.get('uid');
    if (!uid) {
      this.errorMessage.set('Missing purchase order identifier.');
      this.loading.set(false);
      return;
    }
    this.load(uid);
  }

  private load(uid: string): void {
    this.loading.set(true);
    forkJoin({
      order: this.purchaseOrderService.findByUid(uid),
      receipts: this.purchaseOrderService.listReceipts(uid)
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: ({ order, receipts }) => {
        this.order.set(order);
        this.receipts.set(receipts);
      },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load purchase order.')
    });
  }

  back(): void { void this.router.navigate(['/procurement/orders']); }

  openAddLine(): void {
    const o = this.order(); if (!o) return;
    const r = this.modal.open(AddLineComponent, { backdrop: 'static' });
    const inst = r.componentInstance as AddLineComponent;
    inst.orderUid = o.uid;
    inst.supplierUid = o.supplierUid;
    r.closed.subscribe((po: PurchaseOrder | undefined) => { if (po) this.order.set(po); });
  }

  openEditLine(line: PurchaseOrderLine): void {
    const o = this.order(); if (!o) return;
    const r = this.modal.open(AddLineComponent, { backdrop: 'static' });
    const inst = r.componentInstance as AddLineComponent;
    inst.orderUid = o.uid;
    inst.supplierUid = o.supplierUid;
    inst.existing = line;
    r.closed.subscribe((po: PurchaseOrder | undefined) => { if (po) this.order.set(po); });
  }

  removeLine(line: PurchaseOrderLine): void {
    const o = this.order(); if (!o) return;
    if (!globalThis.confirm(`Remove "${line.medicineName ?? line.medicineUid}" from this PO?`)) return;
    this.purchaseOrderService.removeLine(o.uid, line.uid).subscribe({
      next: (po) => this.order.set(po),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not remove line.')
    });
  }

  verify(): void {
    const o = this.order(); if (!o) return;
    if (!globalThis.confirm('Verify this PO? Lines will be locked from further editing.')) return;
    this.runPoAction(this.purchaseOrderService.verify(o.uid), 'Purchase order verified.');
  }

  approve(): void {
    const o = this.order(); if (!o) return;
    if (!globalThis.confirm('Approve this PO for sending to the supplier?')) return;
    this.runPoAction(this.purchaseOrderService.approve(o.uid), 'Purchase order approved.');
  }

  reject(): void {
    const o = this.order(); if (!o) return;
    const reason = globalThis.prompt('Reason for rejecting this PO?')?.trim() ?? null;
    if (reason === null) return;
    this.runPoAction(this.purchaseOrderService.reject(o.uid, reason || null), 'Purchase order rejected.');
  }

  markOrdered(): void {
    const o = this.order(); if (!o) return;
    if (!globalThis.confirm('Submit this PO to the supplier?')) return;
    this.runPoAction(this.purchaseOrderService.markOrdered(o.uid), 'Purchase order sent to supplier.');
  }

  private runPoAction(req$: ReturnType<PurchaseOrderService['verify']>, successMsg: string): void {
    this.busy.set(true);
    this.errorMessage.set(null);
    req$.pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (po) => { this.order.set(po); this.actionMessage.set(successMsg); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'The action failed.')
    });
  }

  cancel(): void {
    const o = this.order(); if (!o) return;
    const reason = globalThis.prompt('Reason for cancelling this PO?')?.trim() ?? null;
    if (reason === null) return;
    this.busy.set(true);
    this.purchaseOrderService.cancel(o.uid, reason || null).pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (po) => { this.order.set(po); this.actionMessage.set('Purchase order cancelled.'); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not cancel PO.')
    });
  }

  openReceive(): void {
    const o = this.order(); if (!o) return;
    const r = this.modal.open(ReceiveGoodsComponent, { size: 'lg', backdrop: 'static' });
    (r.componentInstance as ReceiveGoodsComponent).order = o;
    r.closed.subscribe((receipt: GoodsReceipt | undefined) => {
      if (receipt) {
        // After a receipt, re-fetch both PO (line received qty / status) and receipts list.
        this.actionMessage.set(`Receipt ${receipt.receiptNo} recorded.`);
        this.load(o.uid);
      }
    });
  }

  openCreateInvoice(): void {
    const o = this.order(); if (!o) return;
    const r = this.modal.open(SupplierInvoiceCreateComponent, { size: 'lg', backdrop: 'static' });
    (r.componentInstance as SupplierInvoiceCreateComponent).order = o;
    r.closed.subscribe((inv: SupplierInvoice | undefined) => {
      if (inv) void this.router.navigate(['/procurement/supplier-invoices', inv.uid]);
    });
  }

  // ----- goods receipt actions (the GRN approve is what posts stock + rolls the PO) -----
  verifyReceipt(r: GoodsReceipt): void {
    if (!globalThis.confirm(`Verify receipt ${r.receiptNo}?`)) return;
    this.runGrnAction(this.purchaseOrderService.verifyReceipt(r.uid), `Receipt ${r.receiptNo} verified.`);
  }

  approveReceipt(r: GoodsReceipt): void {
    if (!globalThis.confirm(`Approve receipt ${r.receiptNo}? This posts the goods to store stock and updates the PO.`)) return;
    this.runGrnAction(this.purchaseOrderService.approveReceipt(r.uid), `Receipt ${r.receiptNo} approved — stock posted.`);
  }

  rejectReceipt(r: GoodsReceipt): void {
    const reason = globalThis.prompt(`Reason for rejecting receipt ${r.receiptNo}?`)?.trim() ?? null;
    if (reason === null) return;
    this.runGrnAction(this.purchaseOrderService.rejectReceipt(r.uid, reason || null), `Receipt ${r.receiptNo} rejected.`);
  }

  private runGrnAction(req$: ReturnType<PurchaseOrderService['verifyReceipt']>, successMsg: string): void {
    const o = this.order(); if (!o) return;
    this.busy.set(true);
    this.errorMessage.set(null);
    req$.pipe(finalize(() => this.busy.set(false))).subscribe({
      // Reload the whole PO: approving a GRN advances the PO status + line received qty.
      next: () => { this.actionMessage.set(successMsg); this.load(o.uid); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'The receipt action failed.')
    });
  }

  statusBadgeClass(s: PurchaseOrderStatus): string {
    return 'badge ' + (this.statuses.find((x) => x.value === s)?.badgeClass ?? '');
  }
  statusLabel(s: PurchaseOrderStatus): string {
    return this.statuses.find((x) => x.value === s)?.label ?? s;
  }
  grnStatusBadgeClass(s: GoodsReceiptStatus): string {
    return 'badge ' + (this.grnStatuses.find((x) => x.value === s)?.badgeClass ?? '');
  }
  grnStatusLabel(s: GoodsReceiptStatus): string {
    return this.grnStatuses.find((x) => x.value === s)?.label ?? s;
  }
}
