import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { InvoiceService } from './invoice.service';
import { CREDIT_NOTE_REASONS, CreditNote, CreditNoteReason, Invoice } from './invoice.types';

/** Raise a write-down (credit note) against an invoice — reduces what the patient owes. */
@Component({
  selector: 'app-raise-credit-note',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './raise-credit-note.component.html'
})
export class RaiseCreditNoteComponent implements OnInit {
  @Input({ required: true }) invoice!: Invoice;

  private readonly fb = inject(FormBuilder);
  private readonly invoiceService = inject(InvoiceService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly reasons = CREDIT_NOTE_REASONS;
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    amount:      [0, [Validators.required, Validators.min(0.01)]],
    reason:      ['HARDSHIP' as CreditNoteReason, [Validators.required]],
    description: ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    this.form.patchValue({ amount: this.invoice.balance });
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.invoiceService.raiseCreditNote(this.invoice.uid, {
      amount: Number(raw.amount),
      reason: raw.reason,
      description: raw.description?.trim() || null
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (cn: CreditNote) => this.activeModal.close(cn),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not raise the credit note.')
    });
  }
}
