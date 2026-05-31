import { CommonModule } from '@angular/common';
import { Component, Input, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { NursingChartService } from './nursing-chart.service';
import { DressingEntry, WOUND_STATUSES, WoundStatus } from './nursing-chart.types';

/** Record one wound-dressing observation against an admission. */
@Component({
  selector: 'app-record-dressing-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="modal-header">
      <h5 class="modal-title">Record dressing</h5>
      <button type="button" class="btn-close" (click)="activeModal.dismiss()" aria-label="Close"></button>
    </div>
    <div class="modal-body">
      @if (errorMessage()) {
        <div class="alert alert-danger d-flex align-items-center gap-2"><i class="bi bi-exclamation-circle"></i><span>{{ errorMessage() }}</span></div>
      }
      <form [formGroup]="form" class="row g-3">
        <div class="col-12 col-md-8">
          <label class="form-label" for="d-location">Wound location <span class="text-danger">*</span></label>
          <input id="d-location" type="text" class="form-control" formControlName="woundLocation" maxlength="160">
        </div>
        <div class="col-12 col-md-4">
          <label class="form-label" for="d-status">Wound status <span class="text-danger">*</span></label>
          <select id="d-status" class="form-select" formControlName="woundStatus">
            @for (s of woundStatuses; track s.value) { <option [value]="s.value">{{ s.label }}</option> }
          </select>
        </div>
        <div class="col-12">
          <label class="form-label" for="d-applied">Dressing applied <span class="text-danger">*</span></label>
          <textarea id="d-applied" rows="2" class="form-control" formControlName="dressingApplied" maxlength="500"></textarea>
        </div>
        <div class="col-12">
          <label class="form-label" for="d-notes">Notes</label>
          <textarea id="d-notes" rows="2" class="form-control" formControlName="notes" maxlength="1000"></textarea>
        </div>
      </form>
    </div>
    <div class="modal-footer">
      <button type="button" class="btn btn-light border" (click)="activeModal.dismiss()" [disabled]="busy()">Cancel</button>
      <button type="button" class="btn btn-primary d-flex align-items-center gap-2" (click)="submit()" [disabled]="busy() || form.invalid">
        @if (busy()) { <output class="spinner-border spinner-border-sm" aria-live="polite"><span class="visually-hidden">Saving</span></output> }
        <i class="bi bi-check2"></i>Record
      </button>
    </div>
  `
})
export class RecordDressingModalComponent {
  @Input({ required: true }) admissionUid!: string;

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(NursingChartService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly woundStatuses = WOUND_STATUSES;
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    woundLocation:   ['', [Validators.required, Validators.maxLength(160)]],
    woundStatus:     ['CLEAN' as WoundStatus, [Validators.required]],
    dressingApplied: ['', [Validators.required, Validators.maxLength(500)]],
    notes:           ['', [Validators.maxLength(1000)]]
  });

  submit(): void {
    if (this.busy()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.busy.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.service.recordDressing(this.admissionUid, {
      woundLocation: raw.woundLocation.trim(),
      woundStatus: raw.woundStatus,
      dressingApplied: raw.dressingApplied.trim(),
      notes: raw.notes.trim() || null
    }).pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (entry: DressingEntry) => this.activeModal.close(entry),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not record dressing.')
    });
  }
}
