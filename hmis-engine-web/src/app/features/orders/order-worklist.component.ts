import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import {
  CLINICAL_ORDER_KINDS, CLINICAL_ORDER_STATUSES, ClinicalOrder, ClinicalOrderKind,
  ClinicalOrderStatus, ORDER_URGENCIES, OrderUrgency
} from '../encounter/order/clinical-order.types';
import { EnterResultComponent } from '../encounter/order/enter-result.component';
import { OrderWorklistService } from './order-worklist.service';
import { OrderWorklistRow } from './order-worklist.types';

@Component({
  selector: 'app-order-worklist',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './order-worklist.component.html'
})
export class OrderWorklistComponent implements OnInit {
  private readonly service = inject(OrderWorklistService);
  private readonly modal = inject(NgbModal);

  readonly kinds = CLINICAL_ORDER_KINDS;
  readonly statuses = CLINICAL_ORDER_STATUSES;
  private readonly urgencies = ORDER_URGENCIES;

  readonly rows = signal<OrderWorklistRow[]>([]);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly kindFilter = signal<ClinicalOrderKind | ''>('');
  readonly statusFilter = signal<ClinicalOrderStatus | ''>('');

  private readonly size = 20;

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.service.search({
      kind: this.kindFilter() || undefined,
      status: this.statusFilter() || undefined,
      page: this.page(),
      size: this.size
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (res) => {
        this.rows.set(res.content);
        this.totalPages.set(res.totalPages);
        this.totalElements.set(res.totalElements);
      },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load orders.')
    });
  }

  setKind(k: ClinicalOrderKind | ''): void { this.kindFilter.set(k); this.page.set(0); this.load(); }
  setStatus(s: ClinicalOrderStatus | ''): void { this.statusFilter.set(s); this.page.set(0); this.load(); }

  prev(): void { if (this.page() > 0) { this.page.update((p) => p - 1); this.load(); } }
  next(): void { if (this.page() < this.totalPages() - 1) { this.page.update((p) => p + 1); this.load(); } }

  /** Open the shared result modal for a row (enter / finalize, or view+amend when completed). */
  openResult(row: OrderWorklistRow): void {
    const ref = this.modal.open(EnterResultComponent, { size: 'lg', centered: true, scrollable: true });
    // EnterResultComponent only needs uid + kind off the order.
    ref.componentInstance.order = { uid: row.uid, kind: row.kind } as ClinicalOrder;
    ref.result.then(() => this.load(), () => { /* dismissed */ });
  }

  canEnterResult(s: ClinicalOrderStatus): boolean { return s === 'REQUESTED' || s === 'IN_PROGRESS'; }

  kindLabel(k: ClinicalOrderKind): string { return this.kinds.find((x) => x.value === k)?.label ?? k; }
  kindIcon(k: ClinicalOrderKind): string { return this.kinds.find((x) => x.value === k)?.icon ?? 'bi-card-list'; }
  statusBadgeClass(s: ClinicalOrderStatus): string {
    return 'badge ' + (this.statuses.find((x) => x.value === s)?.badgeClass ?? '');
  }
  statusLabel(s: ClinicalOrderStatus): string { return this.statuses.find((x) => x.value === s)?.label ?? s; }
  urgencyBadgeClass(u: OrderUrgency): string {
    return 'badge ' + (this.urgencies.find((x) => x.value === u)?.badgeClass ?? 'text-bg-light');
  }
}
