import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { NgbDropdownModule, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Subject, debounceTime, distinctUntilChanged, finalize, startWith, switchMap, tap } from 'rxjs';

import { ItemPricesComponent } from '../pricing/item-prices.component';
import { ClinicCliniciansComponent } from './clinic-clinicians.component';
import { ClinicFormComponent } from './clinic-form.component';
import { ClinicService } from './clinic.service';
import { CLINIC_TYPES, Clinic, ClinicType } from './clinic.types';

@Component({
  selector: 'app-clinic-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgbDropdownModule],
  templateUrl: './clinic-list.component.html',
  styleUrl: './clinic-list.component.scss'
})
export class ClinicListComponent {
  private readonly clinicService = inject(ClinicService);
  private readonly modal = inject(NgbModal);

  readonly clinicTypes = CLINIC_TYPES;

  readonly query = new FormControl('', { nonNullable: true });
  readonly activeFilter = signal<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');
  readonly typeFilter = signal<ClinicType | 'ALL'>('ALL');
  readonly page = signal(0);
  readonly pageSize = signal(10);

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly refresh$ = new Subject<void>();

  private readonly searchQuery = toSignal(
    this.query.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      startWith(''),
      tap(() => this.page.set(0))
    ),
    { initialValue: '' }
  );

  private readonly result = toSignal(
    this.refresh$.pipe(
      startWith(void 0),
      switchMap(() => {
        this.loading.set(true);
        this.errorMessage.set(null);
        const activeFilter = this.activeFilter();
        return this.clinicService
          .search({
            query: this.searchQuery() || undefined,
            active:
              activeFilter === 'ALL' ? undefined : activeFilter === 'ACTIVE',
            type: this.typeFilter() === 'ALL' ? undefined : (this.typeFilter() as ClinicType),
            page: this.page(),
            size: this.pageSize(),
            sort: 'name,asc'
          })
          .pipe(
            finalize(() => this.loading.set(false))
          );
      }),
      tap({
        error: (err) => {
          this.errorMessage.set(err?.error?.message ?? 'Could not load clinics.');
          this.loading.set(false);
        }
      }),
      takeUntilDestroyed()
    ),
    { initialValue: null }
  );

  readonly clinics = computed(() => this.result()?.content ?? []);
  readonly totalElements = computed(() => this.result()?.totalElements ?? 0);
  readonly totalPages = computed(() => this.result()?.totalPages ?? 0);

  readonly pageWindow = computed(() => {
    const total = this.totalPages();
    const current = this.page();
    if (total <= 7) {
      return Array.from({ length: total }, (_, i) => i);
    }
    const window: number[] = [];
    const start = Math.max(0, current - 2);
    const end = Math.min(total - 1, current + 2);
    for (let i = start; i <= end; i++) {
      window.push(i);
    }
    return window;
  });

  constructor() {
    // Refresh whenever debounced query or any filter changes
    this.query.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => this.refresh$.next());
  }

  setActiveFilter(value: 'ALL' | 'ACTIVE' | 'INACTIVE'): void {
    this.activeFilter.set(value);
    this.page.set(0);
    this.refresh$.next();
  }

  setTypeFilter(value: ClinicType | 'ALL'): void {
    this.typeFilter.set(value);
    this.page.set(0);
    this.refresh$.next();
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages() || p === this.page()) {
      return;
    }
    this.page.set(p);
    this.refresh$.next();
  }

  changePageSize(size: number): void {
    this.pageSize.set(size);
    this.page.set(0);
    this.refresh$.next();
  }

  openCreate(): void {
    const ref = this.modal.open(ClinicFormComponent, { size: 'lg', backdrop: 'static' });
    ref.closed.subscribe(() => this.refresh$.next());
  }

  openEdit(clinic: Clinic): void {
    const ref = this.modal.open(ClinicFormComponent, { size: 'lg', backdrop: 'static' });
    (ref.componentInstance as ClinicFormComponent).existing = clinic;
    ref.closed.subscribe(() => this.refresh$.next());
  }

  openClinicians(clinic: Clinic): void {
    const ref = this.modal.open(ClinicCliniciansComponent, { size: 'lg', backdrop: 'static' });
    (ref.componentInstance as ClinicCliniciansComponent).clinic = clinic;
  }

  openPrices(clinic: Clinic): void {
    // Manage this clinic's consultation prices (cash + plans, per currency, with band).
    const ref = this.modal.open(ItemPricesComponent, { size: 'lg', backdrop: 'static' });
    const inst = ref.componentInstance as ItemPricesComponent;
    inst.kind = 'CONSULTATION';
    inst.serviceUid = clinic.uid;
    inst.serviceLabel = `${clinic.name} (${clinic.code})`;
  }

  toggleActive(clinic: Clinic): void {
    this.clinicService.setActive(clinic.uid, !clinic.active).subscribe({
      next: () => this.refresh$.next(),
      error: (err) =>
        this.errorMessage.set(err?.error?.message ?? 'Could not update clinic status.')
    });
  }

  delete(clinic: Clinic): void {
    const confirmed = globalThis.confirm(
      `Delete clinic "${clinic.name}"? This cannot be undone.`
    );
    if (!confirmed) {
      return;
    }
    this.clinicService.delete(clinic.uid).subscribe({
      next: () => this.refresh$.next(),
      error: (err) =>
        this.errorMessage.set(err?.error?.message ?? 'Could not delete clinic.')
    });
  }

  typeLabel(type: ClinicType): string {
    return this.clinicTypes.find((t) => t.value === type)?.label ?? type;
  }

  typeBadgeClass(type: ClinicType): string {
    const map: Record<ClinicType, string> = {
      OUTPATIENT: 'text-bg-primary-subtle text-primary border-primary-subtle',
      INPATIENT: 'text-bg-info-subtle text-info-emphasis border-info-subtle',
      SPECIALTY: 'text-bg-warning-subtle text-warning-emphasis border-warning-subtle',
      EMERGENCY: 'text-bg-danger-subtle text-danger-emphasis border-danger-subtle',
      DAYCARE: 'text-bg-success-subtle text-success-emphasis border-success-subtle'
    };
    return `badge border ${map[type]}`;
  }
}
