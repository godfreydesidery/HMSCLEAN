import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NgbDropdownModule, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize, forkJoin } from 'rxjs';

import { AddLineComponent } from './add-line.component';
import { PurchaseOrderService } from './purchase-order.service';
import {
  GoodsReceipt, PURCHASE_ORDER_STATUSES, PurchaseOrder, PurchaseOrderLine, PurchaseOrderStatus
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

  readonly statuses = PURCHASE_ORDER_STATUSES;
  readonly order = signal<PurchaseOrder | null>(null);
  readonly receipts = signal<GoodsReceipt[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly actionMessage = signal<string | null>(null);

  readonly canEdit = computed(() => this.order()?.status === 'DRAFT');
  readonly canOrder = computed(() => {
    const o = this.order();
    return o?.status === 'DRAFT' && o.lines.length > 0;
  });
  readonly canReceive = computed(() => {
    const s = this.order()?.status;
    return s === 'ORDERED' || s === 'PARTIALLY_RECEIVED';
  });
  readonly canCancel = computed(() => {
    const s = this.order()?.status;
    return s != null && s !== 'RECEIVED' && s !== 'CANCELLED';
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

  markOrdered(): void {
    const o = this.order(); if (!o) return;
    if (!globalThis.confirm('Submit this PO to the supplier? Lines will be locked.')) return;
    this.busy.set(true);
    this.purchaseOrderService.markOrdered(o.uid).pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (po) => { this.order.set(po); this.actionMessage.set('Purchase order sent.'); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not submit PO.')
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

  statusBadgeClass(s: PurchaseOrderStatus): string {
    return 'badge ' + (this.statuses.find((x) => x.value === s)?.badgeClass ?? '');
  }
  statusLabel(s: PurchaseOrderStatus): string {
    return this.statuses.find((x) => x.value === s)?.label ?? s;
  }
}
