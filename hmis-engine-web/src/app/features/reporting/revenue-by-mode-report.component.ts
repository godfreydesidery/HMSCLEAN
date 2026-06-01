import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { PAYMENT_METHODS, PaymentMethod } from '../billing/invoice.types';
import { ReportingService } from './reporting.service';
import { RevenueByModeDto } from './reporting.types';

function isoToday(): string {
  return new Date().toISOString().slice(0, 10);
}
function isoMonthStart(): string {
  const d = new Date();
  d.setDate(1);
  return d.toISOString().slice(0, 10);
}

/** Revenue split by payment mode (BILL-5) — how collected cash came in. */
@Component({
  selector: 'app-revenue-by-mode-report',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './revenue-by-mode-report.component.html'
})
export class RevenueByModeReportComponent {
  private readonly reportingService = inject(ReportingService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly result = signal<RevenueByModeDto | null>(null);

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
    this.reportingService.revenueByMode({ from, to })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (r) => this.result.set(r),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load revenue by mode.')
      });
  }

  methodLabel(method: PaymentMethod): string {
    return PAYMENT_METHODS.find((m) => m.value === method)?.label ?? method;
  }
}
