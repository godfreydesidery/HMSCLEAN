import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { Subject, finalize, startWith, switchMap, tap } from 'rxjs';

import { LabBatchService } from './lab-batch.service';
import { LAB_BATCH_STATUSES, LabBatch, LabBatchStatus } from './lab-batch.types';

@Component({
  selector: 'app-lab-batch-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './lab-batch-list.component.html'
})
export class LabBatchListComponent {
  private readonly labBatchService = inject(LabBatchService);
  private readonly router = inject(Router);

  readonly statuses = LAB_BATCH_STATUSES;
  readonly statusFilter = signal<LabBatchStatus | 'ALL'>('ALL');
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
        return this.labBatchService.search({
          status: this.statusFilter() === 'ALL' ? undefined : (this.statusFilter() as LabBatchStatus),
          page: this.page(), size: this.pageSize()
        }).pipe(finalize(() => this.loading.set(false)));
      }),
      tap({ error: (err) => { this.errorMessage.set(err?.error?.message ?? 'Could not load batches.'); this.loading.set(false); } }),
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

  setStatusFilter(v: LabBatchStatus | 'ALL'): void { this.statusFilter.set(v); this.page.set(0); this.refresh$.next(); }
  goToPage(p: number): void { if (p < 0 || p >= this.totalPages() || p === this.page()) return; this.page.set(p); this.refresh$.next(); }
  changePageSize(s: number): void { this.pageSize.set(s); this.page.set(0); this.refresh$.next(); }

  view(b: LabBatch): void { void this.router.navigate(['/encounters/lab-batches', b.uid]); }

  statusBadgeClass(s: LabBatchStatus): string {
    return 'badge ' + (this.statuses.find((x) => x.value === s)?.badgeClass ?? '');
  }
  statusLabel(s: LabBatchStatus): string {
    return this.statuses.find((x) => x.value === s)?.label ?? s;
  }
}
