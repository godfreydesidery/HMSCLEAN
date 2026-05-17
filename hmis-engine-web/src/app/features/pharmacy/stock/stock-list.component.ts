import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize, forkJoin } from 'rxjs';

import { PharmacyService } from '../../masterdata/pharmacies/pharmacy.service';
import { Pharmacy } from '../../masterdata/pharmacies/pharmacy.types';
import { StockEditComponent } from './stock-edit.component';
import { StockService } from './stock.service';
import {
  STOCK_MOVEMENT_KINDS, StockBalance, StockBatch, StockMovement, StockMovementKind
} from './stock.types';

const LOW_STOCK_THRESHOLD = 10;

@Component({
  selector: 'app-stock-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './stock-list.component.html'
})
export class StockListComponent {
  private readonly stockService = inject(StockService);
  private readonly pharmacyService = inject(PharmacyService);
  private readonly modal = inject(NgbModal);
  private readonly fb = inject(FormBuilder);

  readonly movementKinds = STOCK_MOVEMENT_KINDS;
  readonly lowStockThreshold = LOW_STOCK_THRESHOLD;

  readonly pharmacies = signal<Pharmacy[]>([]);
  readonly selectedPharmacyUid = signal<string | null>(null);
  readonly balances = signal<StockBalance[]>([]);
  readonly movements = signal<StockMovement[]>([]);
  readonly expanded = signal<Set<string>>(new Set());
  readonly query = signal('');
  readonly lowOnly = signal(false);
  readonly expiringOnly = signal(false);

  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly actionMessage = signal<string | null>(null);

  readonly pharmacyForm = this.fb.nonNullable.group({
    pharmacyUid: ['', [Validators.required]]
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

  readonly selectedPharmacy = computed(() =>
    this.pharmacies().find((p) => p.uid === this.selectedPharmacyUid()) ?? null);

  constructor() {
    this.pharmacyService.search({ active: true, size: 200, sort: 'name,asc' })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => {
          this.pharmacies.set(page.content);
          if (page.content.length > 0) {
            const first = page.content[0].uid;
            this.pharmacyForm.controls.pharmacyUid.setValue(first);
            this.loadFor(first);
          }
        },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load pharmacies.')
      });

    this.pharmacyForm.controls.pharmacyUid.valueChanges.subscribe((uid) => {
      if (uid) this.loadFor(uid);
    });
  }

  private loadFor(pharmacyUid: string): void {
    this.selectedPharmacyUid.set(pharmacyUid);
    this.loading.set(true);
    this.errorMessage.set(null);
    forkJoin({
      balances: this.stockService.listBalances(pharmacyUid),
      movements: this.stockService.searchMovements({ pharmacyUid, size: 20, sort: 'occurredAt,desc' })
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: ({ balances, movements }) => {
        this.balances.set(balances);
        this.movements.set(movements.content);
      },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load stock.')
    });
  }

  refresh(): void {
    const uid = this.selectedPharmacyUid();
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

  isBatchExpiringSoon(batch: StockBatch): boolean {
    if (!batch.expiresAt) return false;
    const cutoff = new Date();
    cutoff.setDate(cutoff.getDate() + 30);
    return new Date(batch.expiresAt) <= cutoff;
  }

  openReceive(): void {
    const pharm = this.selectedPharmacy(); if (!pharm) return;
    const ref = this.modal.open(StockEditComponent, { backdrop: 'static' });
    const inst = ref.componentInstance as StockEditComponent;
    inst.pharmacyUid = pharm.uid;
    inst.pharmacyName = pharm.name;
    inst.mode = 'receive';
    ref.closed.subscribe((updated) => {
      if (updated) { this.actionMessage.set('Stock received.'); this.refresh(); }
    });
  }

  openAdjustBatch(batch: StockBatch): void {
    const pharm = this.selectedPharmacy(); if (!pharm) return;
    const ref = this.modal.open(StockEditComponent, { backdrop: 'static' });
    const inst = ref.componentInstance as StockEditComponent;
    inst.pharmacyUid = pharm.uid;
    inst.pharmacyName = pharm.name;
    inst.mode = 'adjust';
    inst.prefillBatchUid = batch.uid;
    inst.prefillBatchLabel = `${batch.medicineName ?? batch.medicineUid} · batch ${batch.batchNo}`;
    ref.closed.subscribe((updated) => {
      if (updated) { this.actionMessage.set('Batch adjusted.'); this.refresh(); }
    });
  }

  movementBadgeClass(k: StockMovementKind): string {
    return 'badge d-inline-flex align-items-center gap-1 ' + (this.movementKinds.find((x) => x.value === k)?.badgeClass ?? '');
  }
  movementIcon(k: StockMovementKind): string {
    return this.movementKinds.find((x) => x.value === k)?.icon ?? 'bi-arrow-repeat';
  }
  movementLabel(k: StockMovementKind): string {
    return this.movementKinds.find((x) => x.value === k)?.label ?? k;
  }
}
