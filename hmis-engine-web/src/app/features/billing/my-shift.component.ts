import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { CashierShiftService } from './cashier-shift.service';
import { CashierShift } from './cashier-shift.types';

/**
 * The cashier's own till: open a shift with an opening float, then at end of day
 * close it by declaring the counted cash. The backend computes the expected cash
 * (opening float + this cashier's cash takings during the window) and the variance.
 */
@Component({
  selector: 'app-my-shift',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './my-shift.component.html'
})
export class MyShiftComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(CashierShiftService);

  /** The current OPEN shift (null = none open). */
  readonly shift = signal<CashierShift | null>(null);
  /** The just-closed shift, shown with its reconciliation result. */
  readonly closedResult = signal<CashierShift | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly openForm = this.fb.nonNullable.group({
    openingFloat: [0, [Validators.required, Validators.min(0)]],
    currency: ['TZS', [Validators.required, Validators.pattern(/^[A-Za-z]{3}$/)]]
  });
  readonly closeForm = this.fb.nonNullable.group({
    closingDeclaredAmount: [0, [Validators.required, Validators.min(0)]],
    note: ['', [Validators.maxLength(500)]]
  });

  constructor() { this.load(); }

  private load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.service.currentOpen().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (s) => this.shift.set(s),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load your shift.')
    });
  }

  openShift(): void {
    if (this.openForm.invalid || this.busy()) { this.openForm.markAllAsTouched(); return; }
    this.busy.set(true);
    this.errorMessage.set(null);
    this.closedResult.set(null);
    const raw = this.openForm.getRawValue();
    this.service.open({ openingFloat: Number(raw.openingFloat), currency: raw.currency.trim().toUpperCase() || null })
      .pipe(finalize(() => this.busy.set(false))).subscribe({
        next: (s) => this.shift.set(s),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not open the shift.')
      });
  }

  closeShift(): void {
    if (this.closeForm.invalid || this.busy()) { this.closeForm.markAllAsTouched(); return; }
    if (!globalThis.confirm('Close this shift? This cannot be reopened.')) return;
    this.busy.set(true);
    this.errorMessage.set(null);
    const raw = this.closeForm.getRawValue();
    this.service.close({ closingDeclaredAmount: Number(raw.closingDeclaredAmount), note: raw.note.trim() || null })
      .pipe(finalize(() => this.busy.set(false))).subscribe({
        next: (s) => { this.closedResult.set(s); this.shift.set(null); this.closeForm.reset({ closingDeclaredAmount: 0, note: '' }); },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not close the shift.')
      });
  }
}
