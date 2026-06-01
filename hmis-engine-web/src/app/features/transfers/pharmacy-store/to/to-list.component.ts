import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, finalize, startWith, switchMap, tap } from 'rxjs';

import {
  TRANSFER_DOC_STATUSES, TransferDocStatus, transferDocBadgeClass, transferDocLabel
} from '../../transfer-common.types';
import { ToService } from './to.service';
import { TOSummary } from './to.types';

@Component({
  selector: 'app-to-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './to-list.component.html'
})
export class ToListComponent {
  private readonly toService = inject(ToService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly statuses = TRANSFER_DOC_STATUSES;
  readonly query = new FormControl('', { nonNullable: true });
  readonly statusFilter = signal<TransferDocStatus | 'ALL'>('ALL');
  readonly page = signal(0);
  readonly pageSize = signal(15);
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
        return this.toService.search({
          query: this.searchQuery() || undefined,
          status: this.statusFilter() === 'ALL' ? undefined : (this.statusFilter() as TransferDocStatus),
          page: this.page(), size: this.pageSize(), sort: 'createdAt,desc'
        }).pipe(finalize(() => this.loading.set(false)));
      }),
      tap({ error: (err) => { this.errorMessage.set(err?.error?.message ?? 'Could not load transfer orders.'); this.loading.set(false); } }),
      takeUntilDestroyed()
    ),
    { initialValue: null }
  );

  readonly orders = computed(() => this.result()?.content ?? []);
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

  setStatusFilter(v: TransferDocStatus | 'ALL'): void { this.statusFilter.set(v); this.page.set(0); this.refresh$.next(); }
  goToPage(p: number): void { if (p < 0 || p >= this.totalPages() || p === this.page()) return; this.page.set(p); this.refresh$.next(); }
  changePageSize(s: number): void { this.pageSize.set(s); this.page.set(0); this.refresh$.next(); }

  newOrder(): void { void this.router.navigate(['new'], { relativeTo: this.route }); }
  view(o: TOSummary): void { void this.router.navigate([o.uid], { relativeTo: this.route }); }

  statusBadgeClass(s: TransferDocStatus): string { return transferDocBadgeClass(s); }
  statusLabel(s: TransferDocStatus): string { return transferDocLabel(s); }
}
