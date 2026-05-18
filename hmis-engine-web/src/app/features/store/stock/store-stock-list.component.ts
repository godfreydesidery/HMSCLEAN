import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize, forkJoin } from 'rxjs';

import { StoreService } from '../../masterdata/stores/store.service';
import { Store } from '../../masterdata/stores/store.types';
import { StoreStockEditComponent } from './store-stock-edit.component';
import { StoreStockService } from './store-stock.service';
import {
  STORE_STOCK_MOVEMENT_KINDS, StoreStockBalance, StoreStockBatch,
  StoreStockMovement, StoreStockMovementKind
} from './store-stock.types';

const LOW_STOCK_THRESHOLD = 10;

@Component({
  selector: 'app-store-stock-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './store-stock-list.component.html'
})
export class StoreStockListComponent {
  private readonly stockService = inject(StoreStockService);
  private readonly storeService = inject(StoreService);
  private readonly modal = inject(NgbModal);
  private readonly fb = inject(FormBuilder);

  readonly movementKinds = STORE_STOCK_MOVEMENT_KINDS;
  readonly lowStockThreshold = LOW_STOCK_THRESHOLD;

  readonly stores = signal<Store[]>([]);
  readonly selectedStoreUid = signal<string | null>(null);
  readonly balances = signal<StoreStockBalance[]>([]);
  readonly movements = signal<StoreStockMovement[]>([]);
  readonly expanded = signal<Set<string>>(new Set());
  readonly query = signal('');
  readonly lowOnly = signal(false);
  readonly expiringOnly = signal(false);

  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly actionMessage = signal<string | null>(null);

  readonly storeForm = this.fb.nonNullable.group({
    storeUid: ['', [Validators.required]]
  });

  readonly filteredBalances = computed(() => {
    const q = this.query().trim().toLowerCase();
    let arr = this.balances();
    if (this.lowOnly()) {
      arr = arr.filter((b) => b.totalQuantity <= LOW_STOCK_THRESHOLD);
    }
    if (this.expiringOnly()) {
      const cutoff = new Date();
      cutoff.setDate(cutoff.getDate() + 30);
      arr = arr.filter((b) => b.batchDetails.some((batch) =>
        batch.expiresAt != null && new Date(batch.expiresAt) <= cutoff));
    }
    if (q) {
      arr = arr.filter((b) =>
        (b.medicineName ?? '').toLowerCase().includes(q)
        || (b.medicineCode ?? '').toLowerCase().includes(q));
    }
    return arr;
  });

  readonly selectedStore = computed(() =>
    this.stores().find((s) => s.uid === this.selectedStoreUid()) ?? null);

  constructor() {
    this.storeService.search({ active: true, size: 200, sort: 'name,asc' })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => {
          this.stores.set(page.content);
          if (page.content.length > 0) {
            const first = page.content[0].uid;
            this.storeForm.controls.storeUid.setValue(first);
            this.loadFor(first);
          }
        },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load stores.')
      });

    this.storeForm.controls.storeUid.valueChanges.subscribe((uid) => {
      if (uid) this.loadFor(uid);
    });
  }

  private loadFor(storeUid: string): void {
    this.selectedStoreUid.set(storeUid);
    this.loading.set(true);
    this.errorMessage.set(null);
    forkJoin({
      balances: this.stockService.listBalances(storeUid),
      movements: this.stockService.searchMovements({ storeUid, size: 20, sort: 'occurredAt,desc' })
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: ({ balances, movements }) => {
        this.balances.set(balances);
        this.movements.set(movements.content);
      },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load stock.')
    });
  }

  refresh(): void {
    const uid = this.selectedStoreUid();
    if (uid) this.loadFor(uid);
  }

  setQuery(v: string): void { this.query.set(v); }
  toggleLowOnly(): void { this.lowOnly.update((v) => !v); }
  toggleExpiringOnly(): void { this.expiringOnly.update((v) => !v); }

  isExpanded(medicineUid: string): boolean { return this.expanded().has(medicineUid); }
  toggleExpanded(medicineUid: string): void {
    this.expanded.update((s) => {
      const next = new Set(s);
      if (next.has(medicineUid)) next.delete(medicineUid); else next.add(medicineUid);
      return next;
    });
  }

  isBatchExpiringSoon(batch: StoreStockBatch): boolean {
    if (!batch.expiresAt) return false;
    const cutoff = new Date();
    cutoff.setDate(cutoff.getDate() + 30);
    return new Date(batch.expiresAt) <= cutoff;
  }

  openReceive(): void {
    const store = this.selectedStore(); if (!store) return;
    const ref = this.modal.open(StoreStockEditComponent, { backdrop: 'static' });
    const inst = ref.componentInstance as StoreStockEditComponent;
    inst.storeUid = store.uid;
    inst.storeName = store.name;
    inst.mode = 'receive';
    ref.closed.subscribe((updated) => {
      if (updated) { this.actionMessage.set('Stock received.'); this.refresh(); }
    });
  }

  openAdjustBatch(batch: StoreStockBatch): void {
    const store = this.selectedStore(); if (!store) return;
    const ref = this.modal.open(StoreStockEditComponent, { backdrop: 'static' });
    const inst = ref.componentInstance as StoreStockEditComponent;
    inst.storeUid = store.uid;
    inst.storeName = store.name;
    inst.mode = 'adjust';
    inst.prefillBatchUid = batch.uid;
    inst.prefillBatchLabel = `${batch.medicineName ?? batch.medicineUid} · batch ${batch.batchNo}`;
    ref.closed.subscribe((updated) => {
      if (updated) { this.actionMessage.set('Batch adjusted.'); this.refresh(); }
    });
  }

  movementBadgeClass(k: StoreStockMovementKind): string {
    return 'badge d-inline-flex align-items-center gap-1 ' + (this.movementKinds.find((x) => x.value === k)?.badgeClass ?? '');
  }
  movementIcon(k: StoreStockMovementKind): string {
    return this.movementKinds.find((x) => x.value === k)?.icon ?? 'bi-arrow-repeat';
  }
  movementLabel(k: StoreStockMovementKind): string {
    return this.movementKinds.find((x) => x.value === k)?.label ?? k;
  }
}
