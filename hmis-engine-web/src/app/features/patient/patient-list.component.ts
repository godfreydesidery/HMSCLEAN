import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NgbDropdownModule } from '@ng-bootstrap/ng-bootstrap';
import { Subject, debounceTime, distinctUntilChanged, finalize, startWith, switchMap, tap } from 'rxjs';

import { PatientService } from './patient.service';
import { GENDERS, Gender, PATIENT_TYPES, PAYMENT_TYPES, PatientSummary, PatientType, PaymentType } from './patient.types';

@Component({
  selector: 'app-patient-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgbDropdownModule, RouterLink],
  templateUrl: './patient-list.component.html'
})
export class PatientListComponent {
  private readonly patientService = inject(PatientService);
  private readonly router = inject(Router);

  readonly genders = GENDERS;
  readonly paymentTypes = PAYMENT_TYPES;
  readonly query = new FormControl('', { nonNullable: true });
  readonly activeFilter = signal<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');
  readonly genderFilter = signal<Gender | 'ALL'>('ALL');
  readonly paymentFilter = signal<PaymentType | 'ALL'>('ALL');
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
        return this.patientService.search({
          query: this.searchQuery() || undefined,
          active: this.activeFilter() === 'ALL' ? undefined : this.activeFilter() === 'ACTIVE',
          gender: this.genderFilter() === 'ALL' ? undefined : (this.genderFilter() as Gender),
          paymentType: this.paymentFilter() === 'ALL' ? undefined : (this.paymentFilter() as PaymentType),
          page: this.page(), size: this.pageSize(), sort: 'lastName,asc'
        }).pipe(finalize(() => this.loading.set(false)));
      }),
      tap({ error: (err) => { this.errorMessage.set(err?.error?.message ?? 'Could not load patients.'); this.loading.set(false); } }),
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
  }

  setActiveFilter(v: 'ALL' | 'ACTIVE' | 'INACTIVE'): void { this.activeFilter.set(v); this.page.set(0); this.refresh$.next(); }
  setGenderFilter(v: Gender | 'ALL'): void { this.genderFilter.set(v); this.page.set(0); this.refresh$.next(); }
  setPaymentFilter(v: PaymentType | 'ALL'): void { this.paymentFilter.set(v); this.page.set(0); this.refresh$.next(); }
  goToPage(p: number): void { if (p < 0 || p >= this.totalPages() || p === this.page()) return; this.page.set(p); this.refresh$.next(); }
  changePageSize(s: number): void { this.pageSize.set(s); this.page.set(0); this.refresh$.next(); }

  register(): void { void this.router.navigate(['/patients', 'new']); }
  view(p: PatientSummary): void { void this.router.navigate(['/patients', p.uid]); }
  edit(p: PatientSummary): void { void this.router.navigate(['/patients', p.uid, 'edit']); }

  toggleActive(p: PatientSummary): void {
    this.patientService.setActive(p.uid, !p.active).subscribe({
      next: () => this.refresh$.next(),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not update patient.')
    });
  }

  fullName(p: PatientSummary): string {
    return [p.firstName, p.middleName, p.lastName].filter((s) => !!s && s.length > 0).join(' ');
  }

  age(dob: string): number {
    const birth = new Date(dob);
    const now = new Date();
    let years = now.getFullYear() - birth.getFullYear();
    const m = now.getMonth() - birth.getMonth();
    if (m < 0 || (m === 0 && now.getDate() < birth.getDate())) years--;
    return years;
  }

  initials(p: PatientSummary): string {
    const f = (p.firstName?.[0] ?? '').toUpperCase();
    const l = (p.lastName?.[0] ?? '').toUpperCase();
    return (f + l) || '?';
  }

  genderLabel(g: Gender): string {
    return this.genders.find((x) => x.value === g)?.label ?? g;
  }

  paymentLabel(p: PaymentType): string {
    return this.paymentTypes.find((x) => x.value === p)?.label ?? p;
  }

  typeLabel(t: PatientType): string {
    return PATIENT_TYPES.find((x) => x.value === t)?.label ?? t;
  }

  typeBadgeClass(t: PatientType): string {
    return 'badge ' + (PATIENT_TYPES.find((x) => x.value === t)?.badgeClass ?? '');
  }
}
