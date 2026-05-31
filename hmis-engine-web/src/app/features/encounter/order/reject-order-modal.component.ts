import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

/**
 * Small modal capturing a mandatory rejection reason for a lab/radiology order.
 * Closes with the trimmed reason string; the caller performs the reject call.
 */
@Component({
  selector: 'app-reject-order-modal',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <div class="modal-header">
      <h5 class="modal-title">Reject order</h5>
      <button type="button" class="btn-close" (click)="activeModal.dismiss()" aria-label="Close"></button>
    </div>
    <div class="modal-body">
      <p class="hmis-muted small mb-2">{{ orderLabel }}</p>
      <form [formGroup]="form" novalidate>
        <label class="form-label" for="reject-reason">Reason for rejection</label>
        <textarea id="reject-reason" rows="3" class="form-control" formControlName="reason" maxlength="255"
                  placeholder="e.g. Specimen haemolysed; please re-collect"></textarea>
        <div class="form-text">The ordering clinician sees this; the order can be accepted again later.</div>
      </form>
    </div>
    <div class="modal-footer">
      <button type="button" class="btn btn-light border" (click)="activeModal.dismiss()">Cancel</button>
      <button type="button" class="btn btn-danger" [disabled]="form.invalid" (click)="submit()">
        <i class="bi bi-x-circle me-1"></i>Reject
      </button>
    </div>
  `
})
export class RejectOrderModalComponent {
  /** Optional caption, e.g. "CBC · ORD-2026-000123". */
  orderLabel = '';

  private readonly fb = inject(FormBuilder);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly form = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(255)]]
  });

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.activeModal.close(this.form.getRawValue().reason.trim());
  }
}
