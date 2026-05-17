import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { InvoiceService } from './invoice.service';
import { Invoice, PAYMENT_METHODS, PaymentMethod } from './invoice.types';

@Component({
  selector: 'app-record-payment',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './record-payment.component.html'
})
export class RecordPaymentComponent implements OnInit {
  @Input({ required: true }) invoice!: Invoice;

  private readonly fb = inject(FormBuilder);
  private readonly invoiceService = inject(InvoiceService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly methods = PAYMENT_METHODS;
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    method:    ['CASH' as PaymentMethod, [Validators.required]],
    amount:    [0, [Validators.required, Validators.min(0.01)]],
    currency:  ['TZS', [Validators.required, Validators.pattern(/^[A-Z]{3}$/)]],
    reference: ['',    [Validators.maxLength(80)]],
    note:      ['',    [Validators.maxLength(255)]]
  });

  ngOnInit(): void {
    this.form.patchValue({
      currency: this.invoice.currency,
      amount: this.invoice.balance
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.invoiceService.recordPayment(this.invoice.uid, {
      method: raw.method,
      amount: Number(raw.amount),
      currency: raw.currency.toUpperCase(),
      reference: raw.reference?.trim() || null,
      note: raw.note?.trim() || null
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (updated) => this.activeModal.close(updated),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not record payment.')
    });
  }
}
