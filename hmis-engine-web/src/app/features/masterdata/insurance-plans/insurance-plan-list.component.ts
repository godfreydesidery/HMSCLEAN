import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { NgbDropdownModule, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Subject, debounceTime, distinctUntilChanged, finalize, startWith, switchMap, tap } from 'rxjs';

import { InsuranceProviderService } from '../insurance/insurance.service';
import { InsuranceProvider } from '../insurance/insurance.types';
import { InsurancePlanFormComponent } from './insurance-plan-form.component';
import { InsurancePlanService } from './insurance-plan.service';
import { InsurancePlan } from './insurance-plan.types';

@Component({
  selector: 'app-insurance-plan-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgbDropdownModule],
  templateUrl: './insurance-plan-list.component.html'
})
export class InsurancePlanListComponent {
  private readonly planService = inject(InsurancePlanService);
  private readonly providerService = inject(InsuranceProviderService);
  private readonly modal = inject(NgbModal);

  readonly providers = signal<InsuranceProvider[]>([]);
  readonly query = new FormControl('', { nonNullable: true });
  readonly activeFilter = signal<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');
  readonly providerFilter = signal<string | 'ALL'>('ALL');
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
        return this.planService.search({
          query: this.searchQuery() || undefined,
          active: this.activeFilter() === 'ALL' ? undefined : this.activeFilter() === 'ACTIVE',
          providerUid: this.providerFilter() === 'ALL' ? undefined : this.providerFilter(),
          page: this.page(), size: this.pageSize(), sort: 'name,asc'
        }).pipe(finalize(() => this.loading.set(false)));
      }),
      tap({ error: (err) => { this.errorMessage.set(err?.error?.message ?? 'Could not load plans.'); this.loading.set(false); } }),
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
    this.providerService.search({ active: true, size: 100, sort: 'name,asc' }).subscribe({
      next: (res) => this.providers.set(res.content),
      error: () => { /* ignore — filter list stays empty */ }
    });
  }

  setActiveFilter(v: 'ALL' | 'ACTIVE' | 'INACTIVE'): void { this.activeFilter.set(v); this.page.set(0); this.refresh$.next(); }
  setProviderFilter(v: string | 'ALL'): void { this.providerFilter.set(v); this.page.set(0); this.refresh$.next(); }
  goToPage(p: number): void { if (p < 0 || p >= this.totalPages() || p === this.page()) return; this.page.set(p); this.refresh$.next(); }
  changePageSize(s: number): void { this.pageSize.set(s); this.page.set(0); this.refresh$.next(); }

  openCreate(): void { const r = this.modal.open(InsurancePlanFormComponent, { size: 'lg', backdrop: 'static' }); r.closed.subscribe(() => this.refresh$.next()); }
  openEdit(p: InsurancePlan): void { const r = this.modal.open(InsurancePlanFormComponent, { size: 'lg', backdrop: 'static' }); (r.componentInstance as InsurancePlanFormComponent).existing = p; r.closed.subscribe(() => this.refresh$.next()); }

  toggleActive(p: InsurancePlan): void {
    this.planService.setActive(p.uid, !p.active).subscribe({
      next: () => this.refresh$.next(),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not update plan.')
    });
  }
  delete(p: InsurancePlan): void {
    if (!globalThis.confirm(`Delete plan "${p.name}"? This cannot be undone.`)) return;
    this.planService.delete(p.uid).subscribe({
      next: () => this.refresh$.next(),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not delete plan.')
    });
  }

  providerLabel(uid: string): string {
    return this.providers().find((x) => x.uid === uid)?.name ?? uid;
  }

  coverageCount(p: InsurancePlan): number {
    return [p.coversConsultation, p.coversLab, p.coversRadiology, p.coversProcedure, p.coversMedicine, p.coversAdmission].filter(Boolean).length;
  }
}
