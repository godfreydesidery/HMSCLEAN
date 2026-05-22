import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { PayrollService } from './payroll.service';

@Component({
  selector: 'app-payroll-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './payroll-create.component.html'
})
export class PayrollCreateComponent {
  private readonly fb = inject(FormBuilder);
  private readonly payrollService = inject(PayrollService);
  private readonly router = inject(Router);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    code:      ['', [Validators.required, Validators.maxLength(32)]],
    label:     ['', [Validators.required, Validators.maxLength(80)]],
    startDate: ['', [Validators.required]],
    endDate:   ['', [Validators.required]],
    currency:  ['TZS', [Validators.pattern(/^[A-Z]{3}$/)]],
    note:      ['', [Validators.maxLength(500)]]
  });

  submit(): void {
    if (this.submitting()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const raw = this.form.getRawValue();
    if (raw.endDate < raw.startDate) {
      this.form.controls.endDate.setErrors({ before: true });
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.payrollService.createPeriod({
      code: raw.code.trim(),
      label: raw.label.trim(),
      startDate: raw.startDate,
      endDate: raw.endDate,
      currency: raw.currency || 'TZS',
      note: raw.note?.trim() || null
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (created) => void this.router.navigate(['/hr/payroll', created.uid]),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not create period.')
    });
  }

  cancel(): void {
    void this.router.navigate(['/hr/payroll']);
  }
}
