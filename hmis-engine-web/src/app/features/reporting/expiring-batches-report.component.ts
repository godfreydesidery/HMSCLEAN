import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { ReportingService } from './reporting.service';
import { ExpiringBatchEntry, StockLocationKind } from './reporting.types';

@Component({
  selector: 'app-expiring-batches-report',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './expiring-batches-report.component.html'
})
export class ExpiringBatchesReportComponent {
  private readonly reportingService = inject(ReportingService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly entries = signal<ExpiringBatchEntry[]>([]);

  readonly form = this.fb.nonNullable.group({
    daysAhead: [30, [Validators.required, Validators.min(0)]]
  });

  constructor() {
    this.run();
  }

  run(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const daysAhead = this.form.getRawValue().daysAhead;
    this.loading.set(true);
    this.errorMessage.set(null);
    this.reportingService.expiringBatches(daysAhead)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (rows) => this.entries.set(rows),
        error: (err) => { this.entries.set([]); this.errorMessage.set(err?.error?.message ?? 'Could not load expiring batches.'); }
      });
  }

  locationBadgeClass(kind: StockLocationKind): string {
    return kind === 'PHARMACY'
      ? 'badge text-bg-info-subtle text-info-emphasis border border-info-subtle'
      : 'badge text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle';
  }

  /** True when the batch is already past its expiry date. */
  isExpired(e: ExpiringBatchEntry): boolean {
    if (!e.expiresAt) return false;
    return new Date(e.expiresAt) < new Date(new Date().toDateString());
  }
}
