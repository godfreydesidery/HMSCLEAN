import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { NgbDropdownModule, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Subject, debounceTime, distinctUntilChanged, finalize, startWith, switchMap, tap } from 'rxjs';

import { ItemPricesComponent } from '../pricing/item-prices.component';
import { WardBedsComponent } from './ward-beds.component';
import { WardFormComponent } from './ward-form.component';
import { WardService } from './ward.service';
import { WARD_CATEGORIES, Ward, WardCategory } from './ward.types';

@Component({
  selector: 'app-ward-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgbDropdownModule],
  templateUrl: './ward-list.component.html'
})
export class WardListComponent {
  private readonly wardService = inject(WardService);
  private readonly modal = inject(NgbModal);

  readonly categories = WARD_CATEGORIES;
  readonly query = new FormControl('', { nonNullable: true });
  readonly activeFilter = signal<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');
  readonly categoryFilter = signal<WardCategory | 'ALL'>('ALL');
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
        return this.wardService.search({
          query: this.searchQuery() || undefined,
          active: this.activeFilter() === 'ALL' ? undefined : this.activeFilter() === 'ACTIVE',
          category: this.categoryFilter() === 'ALL' ? undefined : (this.categoryFilter() as WardCategory),
          page: this.page(),
          size: this.pageSize(),
          sort: 'name,asc'
        }).pipe(finalize(() => this.loading.set(false)));
      }),
      tap({ error: (err) => { this.errorMessage.set(err?.error?.message ?? 'Could not load wards.'); this.loading.set(false); } }),
      takeUntilDestroyed()
    ),
    { initialValue: null }
  );

  readonly wards = computed(() => this.result()?.content ?? []);
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
  setCategoryFilter(v: WardCategory | 'ALL'): void { this.categoryFilter.set(v); this.page.set(0); this.refresh$.next(); }
  goToPage(p: number): void { if (p < 0 || p >= this.totalPages() || p === this.page()) return; this.page.set(p); this.refresh$.next(); }
  changePageSize(s: number): void { this.pageSize.set(s); this.page.set(0); this.refresh$.next(); }

  openCreate(): void {
    const r = this.modal.open(WardFormComponent, { size: 'lg', backdrop: 'static' });
    r.closed.subscribe(() => this.refresh$.next());
  }

  openEdit(w: Ward): void {
    const r = this.modal.open(WardFormComponent, { size: 'lg', backdrop: 'static' });
    (r.componentInstance as WardFormComponent).existing = w;
    r.closed.subscribe(() => this.refresh$.next());
  }

  openPrices(w: Ward): void {
    const r = this.modal.open(ItemPricesComponent, { size: 'lg', backdrop: 'static' });
    const inst = r.componentInstance as ItemPricesComponent;
    inst.kind = 'WARD'; inst.serviceUid = w.uid; inst.serviceLabel = `${w.name} (${w.code})`;
  }

  openBeds(w: Ward): void {
    const r = this.modal.open(WardBedsComponent, { size: 'lg', backdrop: 'static' });
    const inst = r.componentInstance as WardBedsComponent;
    inst.wardUid = w.uid; inst.wardName = `${w.name} (${w.code})`;
    r.closed.subscribe(() => this.refresh$.next());
  }

  toggleActive(w: Ward): void {
    this.wardService.setActive(w.uid, !w.active).subscribe({
      next: () => this.refresh$.next(),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not update ward.')
    });
  }

  delete(w: Ward): void {
    if (!globalThis.confirm(`Delete ward "${w.name}"? This cannot be undone.`)) return;
    this.wardService.delete(w.uid).subscribe({
      next: () => this.refresh$.next(),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not delete ward.')
    });
  }

  categoryLabel(c: WardCategory): string {
    return this.categories.find((x) => x.value === c)?.label ?? c;
  }
}
