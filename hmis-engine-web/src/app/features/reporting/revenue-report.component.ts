import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { InvoiceLineKind } from '../billing/invoice.types';
import { ReportingService } from './reporting.service';
import { RevenueSummaryDto } from './reporting.types';

const LINE_KIND_LABELS: Record<InvoiceLineKind, string> = {
  CONSULTATION: 'Consultation',
  LAB_TEST: 'Lab test',
  PROCEDURE: 'Procedure',
  RADIOLOGY: 'Radiology',
  MEDICINE: 'Medicine',
  WARD: 'Ward',
  REGISTRATION: 'Registration',
  CONSUMABLE: 'Consumable'
};

function isoToday(): string {
  return new Date().toISOString().slice(0, 10);
}

function isoMonthStart(): string {
  const d = new Date();
  d.setDate(1);
  return d.toISOString().slice(0, 10);
}

@Component({
  selector: 'app-revenue-report',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './revenue-report.component.html'
})
export class RevenueReportComponent {
  private readonly reportingService = inject(ReportingService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly result = signal<RevenueSummaryDto | null>(null);

  readonly form = this.fb.nonNullable.group({
    from: [isoMonthStart(), [Validators.required]],
    to: [isoToday(), [Validators.required]]
  });

  constructor() {
    this.run();
  }

  run(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { from, to } = this.form.getRawValue();
    this.loading.set(true);
    this.errorMessage.set(null);
    this.reportingService.revenue({ from, to })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (r) => this.result.set(r),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load revenue summary.')
      });
  }

  kindLabel(kind: InvoiceLineKind): string {
    return LINE_KIND_LABELS[kind] ?? kind;
  }
}
