import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { InvoiceService } from './invoice.service';
import { PAYMENT_METHODS, PaymentMethod, PayLinesResult } from './invoice.types';

/**
 * Collect cash for the cashier's ticked lines (legacy confirm-payment modal:
 * Amount Received → Change → Confirm). The collected amount is the total of the
 * selected lines (legacy pays a bill in full); the "amount received" field only
 * drives the change calculation and never changes what is settled.
 */
@Component({
  selector: 'app-collect-payment',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './collect-payment.component.html'
})
export class CollectPaymentComponent implements OnInit {
  @Input({ required: true }) patientUid!: string;
  @Input({ required: true }) currency!: string;
  @Input({ required: true }) lineUids!: string[];
  /** Total outstanding of the ticked lines — the amount that will be collected. */
  @Input({ required: true }) total!: number;

  private readonly fb = inject(FormBuilder);
  private readonly invoiceService = inject(InvoiceService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly methods = PAYMENT_METHODS;
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    method:    ['CASH' as PaymentMethod, [Validators.required]],
    received:  [0, [Validators.required, Validators.min(0)]],
    reference: ['', [Validators.maxLength(80)]],
    note:      ['', [Validators.maxLength(255)]]
  });

  private readonly received = signal(0);
  readonly change = computed(() => this.received() - this.total);

  ngOnInit(): void {
    this.form.patchValue({ received: this.total });
    this.received.set(this.total);
    this.form.controls.received.valueChanges.subscribe((v) => this.received.set(Number(v) || 0));
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    if (this.change() < 0) { this.errorMessage.set('Amount received is less than the total due.'); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.invoiceService.payLines(this.patientUid, {
      method: raw.method,
      currency: this.currency,
      reference: raw.reference?.trim() || null,
      note: raw.note?.trim() || null,
      lineUids: this.lineUids
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (result: PayLinesResult) => this.activeModal.close(result),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not collect payment.')
    });
  }
}
