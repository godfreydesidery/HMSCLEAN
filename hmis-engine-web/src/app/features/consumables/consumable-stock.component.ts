import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { PharmacyService } from '../masterdata/pharmacies/pharmacy.service';
import { Pharmacy } from '../masterdata/pharmacies/pharmacy.types';
import { StoreService } from '../masterdata/stores/store.service';
import { Store } from '../masterdata/stores/store.types';
import { ConsumableMasterdataService, ConsumableStockService } from './consumable.service';
import {
  CONSUMABLE_SOURCE_KINDS, Consumable, ConsumableSourceKind, ConsumableStockBalanceDto
} from './consumable.types';

@Component({
  selector: 'app-consumable-stock',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './consumable-stock.component.html'
})
export class ConsumableStockComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly stockService = inject(ConsumableStockService);
  private readonly consumableService = inject(ConsumableMasterdataService);
  private readonly pharmacyService = inject(PharmacyService);
  private readonly storeService = inject(StoreService);

  readonly sourceKinds = CONSUMABLE_SOURCE_KINDS;

  readonly pharmacies = signal<Pharmacy[]>([]);
  readonly stores = signal<Store[]>([]);
  readonly consumables = signal<Consumable[]>([]);

  readonly sourceKind = signal<ConsumableSourceKind>('PHARMACY');
  readonly sourceUid = signal<string>('');
  readonly balances = signal<ConsumableStockBalanceDto[]>([]);

  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly receiveForm = this.fb.nonNullable.group({
    consumableUid: ['', [Validators.required]],
    quantity:      [1, [Validators.required, Validators.min(1)]],
    note:          ['']
  });

  ngOnInit(): void {
    this.pharmacyService.search({ active: true, size: 200, sort: 'name,asc' })
      .subscribe({ next: (r) => this.pharmacies.set(r.content), error: () => { /* dropdown stays empty */ } });
    this.storeService.search({ active: true, size: 200, sort: 'name,asc' })
      .subscribe({ next: (r) => this.stores.set(r.content), error: () => { /* idem */ } });
    this.consumableService.search({ active: true, size: 500, sort: 'name,asc' })
      .subscribe({ next: (r) => this.consumables.set(r.content), error: () => { /* idem */ } });
  }

  /** Source-kind buttons reset the location + balances. */
  setSourceKind(k: ConsumableSourceKind): void {
    this.sourceKind.set(k);
    this.sourceUid.set('');
    this.balances.set([]);
  }

  /** Source location dropdown change. */
  setSourceUid(uid: string): void {
    this.sourceUid.set(uid);
    if (uid) this.refreshBalances();
    else this.balances.set([]);
  }

  refreshBalances(): void {
    const uid = this.sourceUid();
    if (!uid) return;
    this.loading.set(true);
    this.errorMessage.set(null);
    this.stockService.listBySource(this.sourceKind(), uid)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (rows) => this.balances.set(rows),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load balances.')
      });
  }

  /** Source-aware location options for the dropdown. */
  get locations(): Array<{ uid: string; label: string }> {
    return this.sourceKind() === 'PHARMACY'
      ? this.pharmacies().map((p) => ({ uid: p.uid, label: `${p.code} — ${p.name}` }))
      : this.stores().map((s) => ({ uid: s.uid, label: `${s.code} — ${s.name}` }));
  }

  receive(): void {
    if (!this.sourceUid() || this.submitting()) return;
    if (this.receiveForm.invalid) { this.receiveForm.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.receiveForm.getRawValue();
    this.stockService.receive({
      sourceKind: this.sourceKind(),
      sourceLocationUid: this.sourceUid(),
      consumableUid: raw.consumableUid,
      quantity: Number(raw.quantity),
      note: raw.note?.trim() || null
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: () => { this.receiveForm.reset({ consumableUid: '', quantity: 1, note: '' }); this.refreshBalances(); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not record receipt.')
    });
  }

  adjust(b: ConsumableStockBalanceDto): void {
    const raw = globalThis.prompt(`Signed adjustment for ${b.consumableName || b.consumableUid} (current: ${b.quantity}). Use negative for a debit:`);
    if (!raw) return;
    const delta = Number(raw);
    if (!Number.isInteger(delta) || delta === 0) {
      this.errorMessage.set('Adjustment must be a non-zero integer.');
      return;
    }
    this.errorMessage.set(null);
    this.stockService.adjust({
      sourceKind: this.sourceKind(),
      sourceLocationUid: this.sourceUid(),
      consumableUid: b.consumableUid,
      delta,
      note: 'Manual adjustment'
    }).subscribe({
      next: () => this.refreshBalances(),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Adjustment failed.')
    });
  }
}
