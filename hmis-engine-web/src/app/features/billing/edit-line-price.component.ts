import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { InvoiceService } from './invoice.service';
import { Invoice, InvoiceLine } from './invoice.types';

/** Renegotiate a single line's unit price within its [min,max] band. */
@Component({
  selector: 'app-edit-line-price',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './edit-line-price.component.html'
})
export class EditLinePriceComponent implements OnInit {
  @Input({ required: true }) invoice!: Invoice;
  @Input({ required: true }) line!: InvoiceLine;

  private readonly fb = inject(FormBuilder);
  private readonly invoiceService = inject(InvoiceService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    unitPrice: [0, [Validators.required, Validators.min(0)]]
  });

  ngOnInit(): void {
    const validators = [Validators.required, Validators.min(this.line.minUnitPrice ?? 0)];
    if (this.line.maxUnitPrice != null) validators.push(Validators.max(this.line.maxUnitPrice));
    this.form.controls.unitPrice.setValidators(validators);
    this.form.patchValue({ unitPrice: this.line.unitPrice });
  }

  get qty(): number { return this.line.quantity; }
  get newAmount(): number { return Number(this.form.controls.unitPrice.value) * this.line.quantity; }

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const unitPrice = Number(this.form.getRawValue().unitPrice);
    this.invoiceService.overrideLinePrice(this.invoice.uid, this.line.uid, { unitPrice })
      .pipe(finalize(() => this.submitting.set(false))).subscribe({
        next: (updated: Invoice) => this.activeModal.close(updated),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not change the price.')
      });
  }
}
