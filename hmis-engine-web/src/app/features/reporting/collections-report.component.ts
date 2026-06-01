import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { PAYMENT_METHODS, PaymentMethod } from '../billing/invoice.types';
import { ReportingService } from './reporting.service';
import { CollectionsReportDto } from './reporting.types';

function isoToday(): string {
  return new Date().toISOString().slice(0, 10);
}

/** Per-cashier collections / cash-up (BILL-2). Defaults to today's takings. */
@Component({
  selector: 'app-collections-report',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './collections-report.component.html'
})
export class CollectionsReportComponent {
  private readonly reportingService = inject(ReportingService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly result = signal<CollectionsReportDto | null>(null);

  readonly form = this.fb.nonNullable.group({
    from: [isoToday(), [Validators.required]],
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
    this.reportingService.collections({ from, to })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (r) => this.result.set(r),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load collections.')
      });
  }

  methodLabel(method: PaymentMethod): string {
    return PAYMENT_METHODS.find((m) => m.value === method)?.label ?? method;
  }

  methodSummary(byMethod: CollectionsReportDto['cashiers'][number]['byMethod']): string {
    return byMethod.map((m) => `${this.methodLabel(m.method)} ${m.amount.toLocaleString()}`).join(' · ');
  }
}
