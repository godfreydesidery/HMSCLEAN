import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { NgbDropdownModule, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Subject, debounceTime, distinctUntilChanged, finalize, startWith, switchMap, tap } from 'rxjs';

import { AssignRolesComponent } from './assign-roles.component';
import { ProviderProfileComponent } from './provider-profile.component';
import { ResetPasswordComponent } from './reset-password.component';
import { UserFormComponent } from './user-form.component';
import { UserService } from './user.service';
import { User } from './user.types';

@Component({
  selector: 'app-iam-users',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgbDropdownModule],
  templateUrl: './users.component.html'
})
export class UsersComponent {
  private readonly userService = inject(UserService);
  private readonly modal = inject(NgbModal);

  readonly query = new FormControl('', { nonNullable: true });
  readonly enabledFilter = signal<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');
  readonly page = signal(0);
  readonly pageSize = signal(15);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly actionMessage = signal<string | null>(null);
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
        return this.userService.search({
          query: this.searchQuery() || undefined,
          enabled: this.enabledFilter() === 'ALL' ? undefined : this.enabledFilter() === 'ACTIVE',
          page: this.page(), size: this.pageSize(), sort: 'firstName,asc'
        }).pipe(finalize(() => this.loading.set(false)));
      }),
      tap({ error: (err) => { this.errorMessage.set(err?.error?.message ?? 'Could not load users.'); this.loading.set(false); } }),
      takeUntilDestroyed()
    ),
    { initialValue: null }
  );

  readonly users = computed(() => this.result()?.content ?? []);
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

  setEnabledFilter(v: 'ALL' | 'ACTIVE' | 'INACTIVE'): void {
    this.enabledFilter.set(v); this.page.set(0); this.refresh$.next();
  }
  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages() || p === this.page()) return;
    this.page.set(p);
    this.refresh$.next();
  }
  changePageSize(s: number): void { this.pageSize.set(s); this.page.set(0); this.refresh$.next(); }

  openCreate(): void {
    const ref = this.modal.open(UserFormComponent, { size: 'lg', backdrop: 'static' });
    ref.closed.subscribe((u: User | undefined) => {
      if (u) { this.actionMessage.set('User created.'); this.refresh$.next(); }
    });
  }

  openRoles(u: User): void {
    const ref = this.modal.open(AssignRolesComponent, { size: 'lg', backdrop: 'static' });
    (ref.componentInstance as AssignRolesComponent).user = u;
    ref.closed.subscribe((updated: User | undefined) => {
      if (updated) { this.actionMessage.set('Roles updated.'); this.refresh$.next(); }
    });
  }

  openReset(u: User): void {
    const ref = this.modal.open(ResetPasswordComponent, { backdrop: 'static' });
    (ref.componentInstance as ResetPasswordComponent).user = u;
    ref.closed.subscribe((done) => {
      if (done) { this.actionMessage.set('Password reset.'); this.refresh$.next(); }
    });
  }

  openProviderProfile(u: User): void {
    const ref = this.modal.open(ProviderProfileComponent, { backdrop: 'static' });
    (ref.componentInstance as ProviderProfileComponent).user = u;
    ref.closed.subscribe((saved) => {
      if (saved) { this.actionMessage.set('Provider profile saved.'); }
    });
  }

  isClinician(u: User): boolean {
    return u.roles?.includes('CLINICIAN') ?? false;
  }

  toggleEnabled(u: User): void {
    this.userService.setEnabled(u.uid, !u.enabled).subscribe({
      next: () => { this.actionMessage.set(u.enabled ? 'User disabled.' : 'User enabled.'); this.refresh$.next(); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not update user.')
    });
  }

  unlock(u: User): void {
    this.userService.unlock(u.uid).subscribe({
      next: () => { this.actionMessage.set('Lockout cleared.'); this.refresh$.next(); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not unlock user.')
    });
  }

  initials(u: User): string {
    return ((u.firstName?.[0] ?? '') + (u.lastName?.[0] ?? '')).toUpperCase() || '?';
  }
}
