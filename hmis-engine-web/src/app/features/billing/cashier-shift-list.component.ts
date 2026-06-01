import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, finalize, startWith, switchMap, tap } from 'rxjs';

import { CashierShiftService } from './cashier-shift.service';
import { CASHIER_SHIFT_STATUSES, CashierShift, CashierShiftStatus } from './cashier-shift.types';

@Component({
  selector: 'app-cashier-shift-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './cashier-shift-list.component.html'
})
export class CashierShiftListComponent {
  private readonly service = inject(CashierShiftService);

  readonly statuses = CASHIER_SHIFT_STATUSES;
  readonly username = new FormControl('', { nonNullable: true });
  readonly statusFilter = signal<CashierShiftStatus | 'ALL'>('ALL');
  readonly page = signal(0);
  readonly pageSize = signal(10);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly refresh$ = new Subject<void>();

  private readonly searchName = toSignal(
    this.username.valueChanges.pipe(debounceTime(300), distinctUntilChanged(), startWith(''), tap(() => this.page.set(0))),
    { initialValue: '' }
  );

  private readonly result = toSignal(
    this.refresh$.pipe(
      startWith(void 0),
      switchMap(() => {
        this.loading.set(true);
        this.errorMessage.set(null);
        return this.service.search({
          username: this.searchName() || undefined,
          status: this.statusFilter() === 'ALL' ? undefined : (this.statusFilter() as CashierShiftStatus),
          page: this.page(), size: this.pageSize(), sort: 'openedAt,desc'
        }).pipe(finalize(() => this.loading.set(false)));
      }),
      tap({ error: (err) => { this.errorMessage.set(err?.error?.message ?? 'Could not load shifts.'); this.loading.set(false); } }),
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
    this.username.valueChanges.pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => this.refresh$.next());
  }

  setStatusFilter(v: CashierShiftStatus | 'ALL'): void { this.statusFilter.set(v); this.page.set(0); this.refresh$.next(); }
  goToPage(p: number): void { if (p < 0 || p >= this.totalPages() || p === this.page()) return; this.page.set(p); this.refresh$.next(); }
  changePageSize(s: number): void { this.pageSize.set(s); this.page.set(0); this.refresh$.next(); }

  statusBadgeClass(s: CashierShiftStatus): string { return 'badge ' + (this.statuses.find((x) => x.value === s)?.badgeClass ?? ''); }
  statusLabel(s: CashierShiftStatus): string { return this.statuses.find((x) => x.value === s)?.label ?? s; }
  varianceClass(v: number | null): string { return v == null ? '' : v < 0 ? 'text-danger' : 'text-success'; }
  trackByUid = (_: number, s: CashierShift): string => s.uid;
}
