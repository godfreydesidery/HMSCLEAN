import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NgbDropdownModule, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Subject, finalize, startWith, switchMap, tap } from 'rxjs';

import { InsurancePlanService } from '../insurance-plans/insurance-plan.service';
import { InsurancePlan } from '../insurance-plans/insurance-plan.types';
import { PriceFormComponent } from './price-form.component';
import { ServicePriceService } from './service-price.service';
import { SERVICE_KINDS, ServiceKind, ServicePrice } from './service-price.types';

@Component({
  selector: 'app-price-list',
  standalone: true,
  imports: [CommonModule, NgbDropdownModule],
  templateUrl: './price-list.component.html'
})
export class PriceListComponent {
  private readonly priceService = inject(ServicePriceService);
  private readonly planService = inject(InsurancePlanService);
  private readonly modal = inject(NgbModal);

  readonly serviceKinds = SERVICE_KINDS;
  readonly plans = signal<InsurancePlan[]>([]);
  readonly planFilter = signal<string | 'ALL' | 'CASH'>('ALL');
  readonly kindFilter = signal<ServiceKind | 'ALL'>('ALL');
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
        // 'CASH' filter (planUid null) is not supported by the backend `planUid`
        // parameter; we request all and filter on the client until pricing search
        // grows a dedicated flag.
        return this.priceService.search({
          planUid: planFilter === 'ALL' || planFilter === 'CASH' ? undefined : planFilter,
          kind: this.kindFilter() === 'ALL' ? undefined : (this.kindFilter() as ServiceKind),
          page: this.page(), size: this.pageSize(), sort: 'kind,asc'
        }).pipe(finalize(() => this.loading.set(false)));
      }),
      tap({ error: (err) => { this.errorMessage.set(err?.error?.message ?? 'Could not load prices.'); this.loading.set(false); } }),
      takeUntilDestroyed()
    ),
    { initialValue: null }
  );

  readonly items = computed(() => {
    const all = this.result()?.content ?? [];
    return this.planFilter() === 'CASH' ? all.filter((p) => p.planUid === null) : all;
  });
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
  }

  setPlanFilter(v: string | 'ALL' | 'CASH'): void { this.planFilter.set(v); this.page.set(0); this.refresh$.next(); }
  setKindFilter(v: ServiceKind | 'ALL'): void { this.kindFilter.set(v); this.page.set(0); this.refresh$.next(); }
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
