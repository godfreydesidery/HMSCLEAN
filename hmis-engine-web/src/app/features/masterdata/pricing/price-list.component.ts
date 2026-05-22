import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { NgbDropdownModule, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Subject, debounceTime, distinctUntilChanged, finalize, startWith, switchMap, tap } from 'rxjs';

import { CurrencyService } from '../currencies/currency.service';
import { Currency } from '../currencies/currency.types';
import { InsurancePlanService } from '../insurance-plans/insurance-plan.service';
import { InsurancePlan } from '../insurance-plans/insurance-plan.types';
import { PriceFormComponent } from './price-form.component';
import { ServicePriceService } from './service-price.service';
import { SERVICE_KINDS, ServiceKind, ServicePrice } from './service-price.types';

@Component({
  selector: 'app-price-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgbDropdownModule],
  templateUrl: './price-list.component.html'
})
export class PriceListComponent {
  private readonly priceService = inject(ServicePriceService);
  private readonly planService = inject(InsurancePlanService);
  private readonly currencyService = inject(CurrencyService);
  private readonly modal = inject(NgbModal);

  readonly serviceKinds = SERVICE_KINDS;
  readonly query = new FormControl('', { nonNullable: true });
  readonly plans = signal<InsurancePlan[]>([]);
  readonly currencies = signal<Currency[]>([]);
  readonly planFilter = signal<string | 'ALL' | 'CASH'>('ALL');
  readonly kindFilter = signal<ServiceKind | 'ALL'>('ALL');
  readonly currencyFilter = signal<string | 'ALL'>('ALL');
  readonly page = signal(0);
  readonly pageSize = signal(15);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly refresh$ = new Subject<void>();

  private readonly result = toSignal(
    this.refresh$.pipe(
      startWith(void 0),
      switchMap(() => {
        this.loading.set(true);
        this.errorMessage.set(null);
        const planFilter = this.planFilter();
        return this.priceService.search({
          planUid: planFilter === 'ALL' || planFilter === 'CASH' ? undefined : planFilter,
          cashOnly: planFilter === 'CASH',
          kind: this.kindFilter() === 'ALL' ? undefined : (this.kindFilter() as ServiceKind),
          currency: this.currencyFilter() === 'ALL' ? undefined : this.currencyFilter(),
          query: this.query.value || undefined,
          page: this.page(), size: this.pageSize(), sort: 'kind,asc'
        }).pipe(finalize(() => this.loading.set(false)));
      }),
      tap({ error: (err) => { this.errorMessage.set(err?.error?.message ?? 'Could not load prices.'); this.loading.set(false); } }),
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
    this.planService.search({ active: true, size: 200, sort: 'name,asc' }).subscribe({
      next: (res) => this.plans.set(res.content),
      error: () => { /* ignore */ }
    });
    this.currencyService.search({ active: true, size: 200, sort: 'code,asc' }).subscribe({
      next: (res) => this.currencies.set(res.content),
      error: () => { /* ignore */ }
    });
    this.query.valueChanges.pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => { this.page.set(0); this.refresh$.next(); });
  }

  setPlanFilter(v: string | 'ALL' | 'CASH'): void { this.planFilter.set(v); this.page.set(0); this.refresh$.next(); }
  setKindFilter(v: ServiceKind | 'ALL'): void { this.kindFilter.set(v); this.page.set(0); this.refresh$.next(); }
  setCurrencyFilter(v: string | 'ALL'): void { this.currencyFilter.set(v); this.page.set(0); this.refresh$.next(); }
  goToPage(p: number): void { if (p < 0 || p >= this.totalPages() || p === this.page()) return; this.page.set(p); this.refresh$.next(); }
  changePageSize(s: number): void { this.pageSize.set(s); this.page.set(0); this.refresh$.next(); }

  openCreate(): void {
    const r = this.modal.open(PriceFormComponent, { size: 'lg', backdrop: 'static' });
    r.closed.subscribe(() => this.refresh$.next());
  }

  openEdit(p: ServicePrice): void {
    const r = this.modal.open(PriceFormComponent, { size: 'lg', backdrop: 'static' });
    (r.componentInstance as PriceFormComponent).existing = p;
    r.closed.subscribe(() => this.refresh$.next());
  }

  delete(p: ServicePrice): void {
    if (!globalThis.confirm(`Delete this price entry? This cannot be undone.`)) return;
    this.priceService.delete(p.uid).subscribe({
      next: () => this.refresh$.next(),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not delete price.')
    });
  }

  kindLabel(k: ServiceKind): string {
    return this.serviceKinds.find((x) => x.value === k)?.label ?? k;
  }

  kindIcon(k: ServiceKind): string {
    return this.serviceKinds.find((x) => x.value === k)?.icon ?? 'bi-tag';
  }

  planLabel(uid: string): string {
    return this.plans().find((p) => p.uid === uid)?.name ?? uid;
  }
}
