import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { debounceTime, distinctUntilChanged, finalize } from 'rxjs';

import { WorkingLocationService } from '../../../core/working-location/working-location.service';
import { StockEditComponent } from './stock-edit.component';
import { StockService } from './stock.service';
import {
  STOCK_MOVEMENT_KINDS, StockBalance, StockBatch, StockMovement, StockMovementKind
} from './stock.types';

const LOW_STOCK_THRESHOLD = 10;

@Component({
  selector: 'app-stock-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './stock-list.component.html'
})
export class StockListComponent {
  private readonly stockService = inject(StockService);
  private readonly workingLocation = inject(WorkingLocationService);
  private readonly router = inject(Router);
  private readonly modal = inject(NgbModal);

  /** The pharmacy this workspace is scoped to (legacy "select pharmacy first") — the
   *  single source of truth for the operator's own pharmacy; shown read-only. */
  readonly workingPharmacy = this.workingLocation.workingPharmacy;

  readonly movementKinds = STOCK_MOVEMENT_KINDS;
  readonly lowStockThreshold = LOW_STOCK_THRESHOLD;

  readonly selectedPharmacyUid = signal<string | null>(null);
  readonly balances = signal<StockBalance[]>([]);
  readonly movements = signal<StockMovement[]>([]);
  readonly expanded = signal<Set<string>>(new Set());
  readonly query = new FormControl('', { nonNullable: true });
  readonly lowOnly = signal(false);
  readonly expiringOnly = signal(false);
  readonly page = signal(0);
  readonly pageSize = signal(15);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);

  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly actionMessage = signal<string | null>(null);

  readonly pageWindow = computed(() => {
    const t = this.totalPages(); const c = this.page();
    if (t <= 7) return Array.from({ length: t }, (_, i) => i);
    const w: number[] = []; const s = Math.max(0, c - 2); const e = Math.min(t - 1, c + 2);
    for (let i = s; i <= e; i++) w.push(i);
    return w;
  });

  constructor() {
    // Enforce the "select pharmacy first" workspace: the operator's own pharmacy comes
    // ONLY from the working location (legacy had one Select page, no per-screen picker).
    // With none set, send the user to the Select page rather than operating anywhere.
    const working = this.workingPharmacy();
    if (!working) {
      void this.router.navigate(['/pharmacy/select']);
      return;
    }

    this.selectedPharmacyUid.set(working.uid);
    this.loadBalances();
    this.loadMovements();

    this.query.valueChanges.pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => { this.page.set(0); this.loadBalances(); });
  }

  private loadBalances(): void {
    const uid = this.selectedPharmacyUid();
    if (!uid) return;
    this.loading.set(true);
    this.errorMessage.set(null);
    this.stockService.searchBalances(uid, {
      query: this.query.value || undefined,
      lowOnly: this.lowOnly() || undefined,
      expiringOnly: this.expiringOnly() || undefined,
      page: this.page(), size: this.pageSize(), sort: 'medicineUid,asc'
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (pg) => { this.balances.set(pg.content); this.totalElements.set(pg.totalElements); this.totalPages.set(pg.totalPages); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load stock.')
    });
  }

  private loadMovements(): void {
    const uid = this.selectedPharmacyUid();
    if (!uid) return;
    this.stockService.searchMovements({ pharmacyUid: uid, size: 20, sort: 'occurredAt,desc' }).subscribe({
      next: (pg) => this.movements.set(pg.content),
      error: () => { /* movements are secondary */ }
    });
  }

  refresh(): void { this.loadBalances(); this.loadMovements(); }

  toggleLowOnly(): void { this.lowOnly.update((v) => !v); this.page.set(0); this.loadBalances(); }
  toggleExpiringOnly(): void { this.expiringOnly.update((v) => !v); this.page.set(0); this.loadBalances(); }
  goToPage(p: number): void { if (p < 0 || p >= this.totalPages() || p === this.page()) return; this.page.set(p); this.loadBalances(); }
  changePageSize(s: number): void { this.pageSize.set(s); this.page.set(0); this.loadBalances(); }

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
    const pharm = this.workingPharmacy(); if (!pharm) return;
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
    const pharm = this.workingPharmacy(); if (!pharm) return;
    const ref = this.modal.open(StockEditComponent, { backdrop: 'static' });
    const inst = ref.componentInstance as StockEditComponent;
    inst.pharmacyUid = pharm.uid;
    inst.pharmacyName = pharm.name;
    inst.mode = 'adjust';
    inst.prefillBatchUid = batch.uid;
    inst.prefillBatchLabel = `${batch.medicineName ?? batch.medicineUid} · batch ${batch.batchNo}`;
    inst.prefillMedicineUid = batch.medicineUid;
    ref.closed.subscribe((updated) => {
      if (updated) { this.actionMessage.set('Batch adjusted.'); this.refresh(); }
    });
  }

  openWriteOffBatch(batch: StockBatch): void {
    const pharm = this.workingPharmacy(); if (!pharm) return;
    const ref = this.modal.open(StockEditComponent, { backdrop: 'static' });
    const inst = ref.componentInstance as StockEditComponent;
    inst.pharmacyUid = pharm.uid;
    inst.pharmacyName = pharm.name;
    inst.mode = 'write-off';
    inst.prefillBatchUid = batch.uid;
    inst.prefillBatchLabel = `${batch.medicineName ?? batch.medicineUid} · batch ${batch.batchNo}`;
    inst.prefillMedicineUid = batch.medicineUid;
    ref.closed.subscribe((updated) => {
      if (updated) { this.actionMessage.set('Batch written off.'); this.refresh(); }
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
