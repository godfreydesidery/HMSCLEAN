import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { ClaimService } from './claim.service';
import { Claim } from './claim.types';

@Component({
  selector: 'app-record-settlement',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './record-settlement.component.html'
})
export class RecordSettlementComponent implements OnInit {
  @Input({ required: true }) claim!: Claim;

  private readonly fb = inject(FormBuilder);
  private readonly claimService = inject(ClaimService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    amount:    [0, [Validators.required, Validators.min(0.01)]],
    reference: ['', [Validators.maxLength(80)]],
    note:      ['', [Validators.maxLength(255)]]
  });

  ngOnInit(): void {
    this.form.patchValue({ amount: this.claim.outstanding });
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.claimService.recordSettlement(this.claim.uid, {
      amount: Number(raw.amount),
      reference: raw.reference?.trim() || null,
      note: raw.note?.trim() || null
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (updated) => this.activeModal.close(updated),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not record settlement.')
    });
  }
}
