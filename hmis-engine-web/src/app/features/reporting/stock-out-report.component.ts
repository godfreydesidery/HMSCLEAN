import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { ReportingService } from './reporting.service';
import { StockLocationKind, StockOutEntry } from './reporting.types';

@Component({
  selector: 'app-stock-out-report',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './stock-out-report.component.html'
})
export class StockOutReportComponent {
  private readonly reportingService = inject(ReportingService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly entries = signal<StockOutEntry[]>([]);

  readonly form = this.fb.nonNullable.group({
    threshold: [0, [Validators.required, Validators.min(0)]]
  });

  constructor() {
    this.run();
  }

  run(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const threshold = this.form.getRawValue().threshold;
    this.loading.set(true);
    this.errorMessage.set(null);
    this.reportingService.stockOut(threshold)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (rows) => this.entries.set(rows),
        error: (err) => { this.entries.set([]); this.errorMessage.set(err?.error?.message ?? 'Could not load stock-out report.'); }
      });
  }

  locationBadgeClass(kind: StockLocationKind): string {
    return kind === 'PHARMACY'
      ? 'badge text-bg-info-subtle text-info-emphasis border border-info-subtle'
      : 'badge text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle';
  }
}
