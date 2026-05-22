import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { NgbDropdownModule, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Subject, debounceTime, distinctUntilChanged, finalize, startWith, switchMap, tap } from 'rxjs';

import { ItemPricesComponent } from '../pricing/item-prices.component';
import { RadiologyFormComponent } from './radiology-form.component';
import { RadiologyTypeService } from './radiology.service';
import { RADIOLOGY_MODALITIES, RadiologyModality, RadiologyType } from './radiology.types';

@Component({
  selector: 'app-radiology-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgbDropdownModule],
  templateUrl: './radiology-list.component.html'
})
export class RadiologyListComponent {
  private readonly service = inject(RadiologyTypeService);
  private readonly modal = inject(NgbModal);

  readonly modalities = RADIOLOGY_MODALITIES;
  readonly query = new FormControl('', { nonNullable: true });
  readonly activeFilter = signal<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');
  readonly modalityFilter = signal<RadiologyModality | 'ALL'>('ALL');
  readonly page = signal(0);
  readonly pageSize = signal(10);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly refresh$ = new Subject<void>();

  private readonly searchQuery = toSignal(
    this.query.valueChanges.pipe(debounceTime(300), distinctUntilChanged(), startWith(''), tap(() => this.page.set(0))),
    { initialValue: '' }
  );

  private readonly result = toSignal(
    this.refresh$.pipe(
      startWith(void 0),
      switchMap(() => {
        this.loading.set(true);
        this.errorMessage.set(null);
        return this.service.search({
          query: this.searchQuery() || undefined,
          active: this.activeFilter() === 'ALL' ? undefined : this.activeFilter() === 'ACTIVE',
          modality: this.modalityFilter() === 'ALL' ? undefined : (this.modalityFilter() as RadiologyModality),
          page: this.page(), size: this.pageSize(), sort: 'name,asc'
        }).pipe(finalize(() => this.loading.set(false)));
      }),
      tap({ error: (err) => { this.errorMessage.set(err?.error?.message ?? 'Could not load radiology types.'); this.loading.set(false); } }),
      takeUntilDestroyed()
    ),
    { initialValue: null }
  );

  readonly items = computed(() => this.result()?.content ?? []);
  readonly totalElements = computed(() => this.result()?.totalElements ?? 0);
  readonly totalPages = computed(() => this.result()?.totalPages ?? 0);
  readonly pageWindow = computed(() => {
    const t = this.totalPages(); const c = this.page();
    if (t <= 7) return Array.from({ length: t }, (_, i) => i);
    const w: number[] = []; const s = Math.max(0, c - 2); const e = Math.min(t - 1, c + 2);
    for (let i = s; i <= e; i++) w.push(i);
    return w;
  });

  constructor() {
    this.query.valueChanges.pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => this.refresh$.next());
  }

  setActiveFilter(v: 'ALL' | 'ACTIVE' | 'INACTIVE'): void { this.activeFilter.set(v); this.page.set(0); this.refresh$.next(); }
  setModalityFilter(v: RadiologyModality | 'ALL'): void { this.modalityFilter.set(v); this.page.set(0); this.refresh$.next(); }
  goToPage(p: number): void { if (p < 0 || p >= this.totalPages() || p === this.page()) return; this.page.set(p); this.refresh$.next(); }
  changePageSize(s: number): void { this.pageSize.set(s); this.page.set(0); this.refresh$.next(); }

  openCreate(): void { const r = this.modal.open(RadiologyFormComponent, { size: 'lg', backdrop: 'static' }); r.closed.subscribe(() => this.refresh$.next()); }
  openEdit(rt: RadiologyType): void { const r = this.modal.open(RadiologyFormComponent, { size: 'lg', backdrop: 'static' }); (r.componentInstance as RadiologyFormComponent).existing = rt; r.closed.subscribe(() => this.refresh$.next()); }
  openPrices(rt: RadiologyType): void {
    const r = this.modal.open(ItemPricesComponent, { size: 'lg', backdrop: 'static' });
    const inst = r.componentInstance as ItemPricesComponent;
    inst.kind = 'RADIOLOGY'; inst.serviceUid = rt.uid; inst.serviceLabel = `${rt.name} (${rt.code})`;
  }

  toggleActive(rt: RadiologyType): void {
    this.service.setActive(rt.uid, !rt.active).subscribe({
      next: () => this.refresh$.next(),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not update radiology.')
    });
  }
  delete(rt: RadiologyType): void {
    if (!globalThis.confirm(`Delete "${rt.name}"? This cannot be undone.`)) return;
    this.service.delete(rt.uid).subscribe({
      next: () => this.refresh$.next(),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not delete radiology.')
    });
  }

  modalityLabel(m: RadiologyModality): string {
    return this.modalities.find((x) => x.value === m)?.label ?? m;
  }
}
