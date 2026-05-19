import { CommonModule } from '@angular/common';
import { Component, Input, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { EmployeeService } from './employee.service';
import { Employee } from './employee.types';

@Component({
  selector: 'app-terminate-employee-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="modal-header">
      <div>
        <h5 class="modal-title">Terminate employee</h5>
        <div class="hmis-muted small mt-1">{{ employeeLabel }}</div>
      </div>
      <button type="button" class="btn-close" (click)="activeModal.dismiss()" aria-label="Close"></button>
    </div>
    <div class="modal-body">
      @if (errorMessage()) {
        <div class="alert alert-danger d-flex align-items-center gap-2" role="alert"><i class="bi bi-exclamation-circle"></i><span>{{ errorMessage() }}</span></div>
      }
      <div class="alert alert-warning d-flex align-items-start gap-2 small">
        <i class="bi bi-exclamation-triangle mt-1"></i>
        <div>Termination is a one-way transition — the status cannot be changed afterwards. Payroll items already raised remain attached to the employee for audit.</div>
      </div>
      <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <div class="mb-3">
          <label class="form-label" for="tm-date">Termination date <span class="text-danger">*</span></label>
          <input id="tm-date" name="terminationDate" type="date" class="form-control" formControlName="terminationDate" [class.is-invalid]="form.controls.terminationDate.invalid && form.controls.terminationDate.touched">
        </div>
        <div class="mb-2">
          <label class="form-label" for="tm-reason">Reason</label>
          <textarea id="tm-reason" name="reason" rows="3" class="form-control" formControlName="reason" maxlength="500" placeholder="e.g. Voluntary resignation, contract end, dismissal for cause…"></textarea>
        </div>
      </form>
    </div>
    <div class="modal-footer">
      <button type="button" class="btn btn-light border" (click)="activeModal.dismiss()" [disabled]="submitting()">Cancel</button>
      <button type="button" class="btn btn-danger d-flex align-items-center gap-2" (click)="submit()" [disabled]="submitting()">
        @if (submitting()) { <output class="spinner-border spinner-border-sm" aria-live="polite"><span class="visually-hidden">Saving</span></output> }
        <i class="bi bi-person-x"></i>Terminate
      </button>
    </div>
  `
})
export class TerminateEmployeeModalComponent {
  @Input({ required: true }) employeeUid!: string;
  @Input({ required: true }) employeeLabel!: string;

  private readonly fb = inject(FormBuilder);
  private readonly employeeService = inject(EmployeeService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    terminationDate: ['', [Validators.required]],
    reason: ['', [Validators.maxLength(500)]]
  });

  submit(): void {
    if (this.submitting()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.employeeService.terminate(this.employeeUid, {
      terminationDate: raw.terminationDate,
      reason: raw.reason.trim() || null
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (e: Employee) => this.activeModal.close(e),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not terminate employee.')
    });
  }
}
