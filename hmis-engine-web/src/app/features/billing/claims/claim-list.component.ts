import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Subject, debounceTime, distinctUntilChanged, finalize, startWith, switchMap, tap } from 'rxjs';

import { AssembleClaimComponent } from './assemble-claim.component';
import { ClaimService } from './claim.service';
import { CLAIM_STATUSES, Claim, ClaimStatus, ClaimSummary } from './claim.types';

@Component({
  selector: 'app-claim-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './claim-list.component.html'
})
export class ClaimListComponent {
  private readonly claimService = inject(ClaimService);
  private readonly router = inject(Router);
  private readonly modal = inject(NgbModal);

  readonly statuses = CLAIM_STATUSES;
  readonly membership = new FormControl('', { nonNullable: true });
  readonly statusFilter = signal<ClaimStatus | 'ALL'>('ALL');
  readonly page = signal(0);
  readonly pageSize = signal(15);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly refresh$ = new Subject<void>();

  private readonly membershipQuery = toSignal(
    this.membership.valueChanges.pipe(debounceTime(300), distinctUntilChanged(), startWith(''), tap(() => this.page.set(0))),
    { initialValue: '' }
  );

  private readonly result = toSignal(
    this.refresh$.pipe(
      startWith(void 0),
      switchMap(() => {
        this.loading.set(true);
        this.errorMessage.set(null);
        return this.claimService.search({
          membershipNo: this.membershipQuery() || undefined,
          status: this.statusFilter() === 'ALL' ? undefined : (this.statusFilter() as ClaimStatus),
          page: this.page(), size: this.pageSize(), sort: 'createdAt,desc'
        }).pipe(finalize(() => this.loading.set(false)));
      }),
      tap({ error: (err) => { this.errorMessage.set(err?.error?.message ?? 'Could not load claims.'); this.loading.set(false); } }),
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
    this.membership.valueChanges.pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => this.refresh$.next());
  }

  setStatusFilter(v: ClaimStatus | 'ALL'): void { this.statusFilter.set(v); this.page.set(0); this.refresh$.next(); }
  goToPage(p: number): void { if (p < 0 || p >= this.totalPages() || p === this.page()) return; this.page.set(p); this.refresh$.next(); }
  changePageSize(s: number): void { this.pageSize.set(s); this.page.set(0); this.refresh$.next(); }

  view(s: ClaimSummary): void { void this.router.navigate(['/billing/claims', s.uid]); }

  assemble(): void {
    const ref = this.modal.open(AssembleClaimComponent, { backdrop: 'static' });
    ref.closed.subscribe((claim: Claim | undefined) => {
      if (claim) void this.router.navigate(['/billing/claims', claim.uid]);
    });
  }

  statusBadgeClass(s: ClaimStatus): string {
    return 'badge ' + (this.statuses.find((x) => x.value === s)?.badgeClass ?? '');
  }
  statusLabel(s: ClaimStatus): string {
    return this.statuses.find((x) => x.value === s)?.label ?? s;
  }
}
