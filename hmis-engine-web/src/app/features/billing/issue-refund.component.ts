import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { InvoiceService } from './invoice.service';
import { Invoice, PAYMENT_METHODS, PaymentMethod, REFUND_REASONS, Refund, RefundReason } from './invoice.types';

/** Return cash already paid on an invoice (capped at amount paid). */
@Component({
  selector: 'app-issue-refund',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './issue-refund.component.html'
})
export class IssueRefundComponent implements OnInit {
  @Input({ required: true }) invoice!: Invoice;

  private readonly fb = inject(FormBuilder);
  private readonly invoiceService = inject(InvoiceService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly methods = PAYMENT_METHODS;
  readonly reasons = REFUND_REASONS;
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    amount:      [0, [Validators.required, Validators.min(0.01)]],
    method:      ['CASH' as PaymentMethod, [Validators.required]],
    reason:      ['OVERPAYMENT' as RefundReason, [Validators.required]],
    reference:   ['', [Validators.maxLength(80)]],
    description: ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    this.form.patchValue({ amount: this.invoice.totalPaid });
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.invoiceService.raiseRefund(this.invoice.uid, {
      amount: Number(raw.amount),
      method: raw.method,
      reason: raw.reason,
      reference: raw.reference?.trim() || null,
      description: raw.description?.trim() || null
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (r: Refund) => this.activeModal.close(r),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not issue the refund.')
    });
  }
}
