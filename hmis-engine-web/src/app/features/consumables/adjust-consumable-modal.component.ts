import { CommonModule } from '@angular/common';
import { Component, Input, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { ConsumableStockService } from './consumable.service';
import { ConsumableSourceKind, ConsumableStockBalanceDto } from './consumable.types';

/** Non-zero-integer validator for the signed delta. */
function nonZeroInteger(control: AbstractControl): ValidationErrors | null {
  const v = control.value;
  if (v === null || v === '' || v === undefined) return null; // `required` handles emptiness
  const n = Number(v);
  if (!Number.isInteger(n) || n === 0) return { nonZeroInteger: true };
  return null;
}

@Component({
  selector: 'app-adjust-consumable-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './adjust-consumable-modal.component.html'
})
export class AdjustConsumableModalComponent {
  @Input({ required: true }) sourceKind!: ConsumableSourceKind;
  @Input({ required: true }) sourceUid!: string;
  @Input({ required: true }) balance!: ConsumableStockBalanceDto;

  private readonly fb = inject(FormBuilder);
  private readonly stockService = inject(ConsumableStockService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    delta: [null as number | null, [Validators.required, nonZeroInteger]],
    note:  ['', [Validators.maxLength(500)]]
  });

  /** Projected on-hand after applying the typed delta — purely informational. */
  get projected(): number | null {
    const d = this.form.controls.delta.value;
    if (d === null || !Number.isInteger(Number(d))) return null;
    return this.balance.quantity + Number(d);
  }

  submit(): void {
    if (this.submitting()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const raw = this.form.getRawValue();
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.stockService.adjust({
      sourceKind:        this.sourceKind,
      sourceLocationUid: this.sourceUid,
      consumableUid:     this.balance.consumableUid,
      delta:             Number(raw.delta),
      note:              raw.note?.trim() || null
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (updated) => this.activeModal.close(updated),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Adjustment failed.')
    });
  }
}
