import { CommonModule } from '@angular/common';
import { Component, Input, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { ClaimService } from './claim.service';
import { Claim } from './claim.types';

@Component({
  selector: 'app-reject-claim',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './reject-claim.component.html'
})
export class RejectClaimComponent {
  @Input({ required: true }) claim!: Claim;

  private readonly fb = inject(FormBuilder);
  private readonly claimService = inject(ClaimService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(255)]]
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.claimService.reject(this.claim.uid, { reason: raw.reason.trim() })
      .pipe(finalize(() => this.submitting.set(false))).subscribe({
        next: (updated) => this.activeModal.close(updated),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not reject claim.')
      });
  }
}
