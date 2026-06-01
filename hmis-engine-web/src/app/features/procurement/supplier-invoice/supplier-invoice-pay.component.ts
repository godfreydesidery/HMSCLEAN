import { CommonModule } from '@angular/common';
import { Component, Input, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { SupplierInvoiceService } from './supplier-invoice.service';
import { SUPPLIER_PAYMENT_METHODS, SupplierInvoice, SupplierPaymentMethod } from './supplier-invoice.types';

/** Mark an APPROVED supplier invoice as paid (records the payment method + reference). */
@Component({
  selector: 'app-supplier-invoice-pay',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './supplier-invoice-pay.component.html'
})
export class SupplierInvoicePayComponent {
  @Input({ required: true }) invoice!: SupplierInvoice;

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(SupplierInvoiceService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly methods = SUPPLIER_PAYMENT_METHODS;
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    method:    ['BANK_TRANSFER' as SupplierPaymentMethod, [Validators.required]],
    reference: ['', [Validators.maxLength(120)]]
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.service.pay(this.invoice.uid, { method: raw.method, reference: raw.reference?.trim() || null })
      .pipe(finalize(() => this.submitting.set(false))).subscribe({
        next: (updated) => this.activeModal.close(updated),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not mark the invoice paid.')
      });
  }
}
