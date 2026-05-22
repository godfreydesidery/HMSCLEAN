import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { debounceTime, distinctUntilChanged, finalize } from 'rxjs';

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

  readonly pharmacyForm = this.fb.nonNullable.group({
    pharmacyUid: ['', [Validators.required]]
  });

  readonly selectedPharmacy = computed(() =>
    this.pharmacies().find((p) => p.uid === this.selectedPharmacyUid()) ?? null);

  readonly pageWindow = computed(() => {
    const t = this.totalPages(); const c = this.page();
    if (t <= 7) return Array.from({ length: t }, (_, i) => i);
    const w: number[] = []; const s = Math.max(0, c - 2); const e = Math.min(t - 1, c + 2);
    for (let i = s; i <= e; i++) w.push(i);
    return w;
  });

  constructor() {
    this.pharmacyService.search({ active: true, size: 200, sort: 'name,asc' })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => {
          this.pharmacies.set(page.content);
          if (page.content.length > 0) {
            this.pharmacyForm.controls.pharmacyUid.setValue(page.content[0].uid);
            this.selectPharmacy(page.content[0].uid);
          }
        },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load pharmacies.')
      });

    this.pharmacyForm.controls.pharmacyUid.valueChanges.subscribe((uid) => {
      if (uid) this.selectPharmacy(uid);
    });

    this.query.valueChanges.pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => { this.page.set(0); this.loadBalances(); });
  }

  private selectPharmacy(uid: string): void {
    this.selectedPharmacyUid.set(uid);
    this.page.set(0);
    this.loadBalances();
    this.loadMovements();
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
    inst.prefillMedicineUid = batch.medicineUid;
    ref.closed.subscribe((updated) => {
      if (updated) { this.actionMessage.set('Batch adjusted.'); this.refresh(); }
    });
  }

  openWriteOffBatch(batch: StockBatch): void {
    const pharm = this.selectedPharmacy(); if (!pharm) return;
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
