import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, finalize, startWith, switchMap, tap } from 'rxjs';

import { AuditService, LOGIN_OUTCOMES, LoginOutcome } from './audit.service';

@Component({
  selector: 'app-audit-log',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './audit-log.component.html'
})
export class AuditLogComponent {
  private readonly auditService = inject(AuditService);

  readonly outcomes = LOGIN_OUTCOMES;
  readonly username = new FormControl('', { nonNullable: true });
  readonly outcomeFilter = signal<LoginOutcome | 'ALL'>('ALL');
  readonly page = signal(0);
  readonly pageSize = signal(25);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly refresh$ = new Subject<void>();

  private readonly searchUsername = toSignal(
    this.username.valueChanges.pipe(debounceTime(300), distinctUntilChanged(), startWith(''), tap(() => this.page.set(0))),
    { initialValue: '' }
  );

  private readonly result = toSignal(
    this.refresh$.pipe(
      startWith(void 0),
      switchMap(() => {
        this.loading.set(true);
        this.errorMessage.set(null);
        return this.auditService.searchLoginAttempts({
          username: this.searchUsername() || undefined,
          outcome: this.outcomeFilter() === 'ALL' ? undefined : (this.outcomeFilter() as LoginOutcome),
          page: this.page(), size: this.pageSize(), sort: 'attemptedAt,desc'
        }).pipe(finalize(() => this.loading.set(false)));
      }),
      tap({ error: (err) => { this.errorMessage.set(err?.error?.message ?? 'Could not load audit log.'); this.loading.set(false); } }),
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

  setOutcomeFilter(v: LoginOutcome | 'ALL'): void { this.outcomeFilter.set(v); this.page.set(0); this.refresh$.next(); }
  goToPage(p: number): void { if (p < 0 || p >= this.totalPages() || p === this.page()) return; this.page.set(p); this.refresh$.next(); }
  changePageSize(s: number): void { this.pageSize.set(s); this.page.set(0); this.refresh$.next(); }

  outcomeBadgeClass(o: LoginOutcome): string {
    return 'badge ' + (this.outcomes.find((x) => x.value === o)?.badgeClass ?? '');
  }
  outcomeLabel(o: LoginOutcome): string {
    return this.outcomes.find((x) => x.value === o)?.label ?? o;
  }
}
