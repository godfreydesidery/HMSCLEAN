import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { PayrollComponentService } from './payroll-component.service';
import { COMPONENT_TYPES, PayrollComponent } from './payroll-component.types';

@Component({
  selector: 'app-payroll-component-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './payroll-component-list.component.html'
})
export class PayrollComponentListComponent implements OnInit {
  private readonly service = inject(PayrollComponentService);

  readonly types = COMPONENT_TYPES;
  readonly components = signal<PayrollComponent[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.service.search({ size: 200, sort: 'sortOrder,asc' })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.components.set(page.content),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load components.')
      });
  }

  toggleActive(c: PayrollComponent): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.errorMessage.set(null);
    this.service.setActive(c.uid, !c.active)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: () => this.load(),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not update component.')
      });
  }

  remove(c: PayrollComponent): void {
    if (this.busy()) return;
    if (!globalThis.confirm(`Delete component "${c.name}" (${c.code})?`)) return;
    this.busy.set(true);
    this.errorMessage.set(null);
    this.service.delete(c.uid)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: () => this.load(),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not delete component.')
      });
  }

  typeBadgeClass(t: string): string {
    return 'badge ' + (this.types.find((x) => x.value === t)?.badgeClass ?? 'bg-secondary');
  }

  formula(c: PayrollComponent): string {
    switch (c.method) {
      case 'FIXED':   return `Fixed ${c.fixedAmount ?? '0'}`;
      case 'PERCENT': return `${pct(c.percentRate)} of ${c.base === 'GROSS' ? 'gross' : 'basic'}`;
      case 'BAND':    return `${c.bands.length} band(s) on ${c.base === 'GROSS' ? 'gross' : 'basic'}`;
      default:        return '';
    }
  }
}

function pct(rate: string | null): string {
  if (rate === null || rate === '') return '0%';
  const n = Number(rate);
  return Number.isFinite(n) ? `${+(n * 100).toFixed(4)}%` : `${rate}`;
}
