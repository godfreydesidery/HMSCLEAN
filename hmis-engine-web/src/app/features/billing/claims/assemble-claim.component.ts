import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { ClaimService } from './claim.service';
import { Claim } from './claim.types';

@Component({
  selector: 'app-assemble-claim',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './assemble-claim.component.html'
})
export class AssembleClaimComponent {
  private readonly fb = inject(FormBuilder);
  private readonly claimService = inject(ClaimService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    payerPlanUid: ['', [Validators.required]],
    membershipNo: ['', [Validators.required, Validators.maxLength(80)]]
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.claimService.assemble({
      payerPlanUid: raw.payerPlanUid.trim(),
      membershipNo: raw.membershipNo.trim()
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (claim: Claim) => this.activeModal.close(claim),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not assemble claim.')
    });
  }
}
