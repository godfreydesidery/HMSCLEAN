import { CommonModule } from '@angular/common';
import { Component, Input, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { NursingChartService } from './nursing-chart.service';
import { CareActivityEntry } from './nursing-chart.types';

/** Record one per-shift care-activity entry (tasks done + bedside blood sugar). */
@Component({
  selector: 'app-record-care-activity-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="modal-header">
      <h5 class="modal-title">Record care activity</h5>
      <button type="button" class="btn-close" (click)="activeModal.dismiss()" aria-label="Close"></button>
    </div>
    <div class="modal-body">
      @if (errorMessage()) {
        <div class="alert alert-danger d-flex align-items-center gap-2"><i class="bi bi-exclamation-circle"></i><span>{{ errorMessage() }}</span></div>
      }
      <form [formGroup]="form" class="row g-3">
        <div class="col-12">
          <div class="d-flex flex-wrap gap-4">
            <div class="form-check">
              <input id="ca-feed" type="checkbox" class="form-check-input" formControlName="feedingDone">
              <label class="form-check-label" for="ca-feed">Feeding</label>
            </div>
            <div class="form-check">
              <input id="ca-pos" type="checkbox" class="form-check-input" formControlName="positionChanged">
              <label class="form-check-label" for="ca-pos">Repositioned</label>
            </div>
            <div class="form-check">
              <input id="ca-bath" type="checkbox" class="form-check-input" formControlName="bedBathDone">
              <label class="form-check-label" for="ca-bath">Bed bath</label>
            </div>
          </div>
        </div>
        <div class="col-12 col-md-6">
          <label class="form-label" for="ca-rbs">Random blood sugar (mmol/L)</label>
          <input id="ca-rbs" type="number" min="0" max="100" step="0.1" class="form-control" formControlName="randomBloodSugarMmol">
        </div>
        <div class="col-12 col-md-6">
          <label class="form-label" for="ca-fbs">Fasting blood sugar (mmol/L)</label>
          <input id="ca-fbs" type="number" min="0" max="100" step="0.1" class="form-control" formControlName="fastingBloodSugarMmol">
        </div>
        <div class="col-12">
          <label class="form-label" for="ca-notes">Notes</label>
          <textarea id="ca-notes" rows="2" class="form-control" formControlName="notes" maxlength="500"></textarea>
        </div>
      </form>
    </div>
    <div class="modal-footer">
      <button type="button" class="btn btn-light border" (click)="activeModal.dismiss()" [disabled]="busy()">Cancel</button>
      <button type="button" class="btn btn-primary d-flex align-items-center gap-2" (click)="submit()" [disabled]="busy() || !hasAnything()">
        @if (busy()) { <output class="spinner-border spinner-border-sm" aria-live="polite"><span class="visually-hidden">Saving</span></output> }
        <i class="bi bi-check2"></i>Record
      </button>
    </div>
  `
})
export class RecordCareActivityModalComponent {
  @Input({ required: true }) admissionUid!: string;

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(NursingChartService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.group({
    feedingDone:           [false],
    positionChanged:       [false],
    bedBathDone:           [false],
    randomBloodSugarMmol:  [null as number | null, [Validators.min(0), Validators.max(100)]],
    fastingBloodSugarMmol: [null as number | null, [Validators.min(0), Validators.max(100)]],
    notes:                 ['', [Validators.maxLength(500)]]
  });

  private readonly value = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });

  readonly hasAnything = computed(() => {
    const v = this.value();
    return !!v.feedingDone || !!v.positionChanged || !!v.bedBathDone
      || num(v.randomBloodSugarMmol) !== null || num(v.fastingBloodSugarMmol) !== null
      || !!(v.notes && v.notes.trim());
  });

  submit(): void {
    if (this.busy() || !this.hasAnything()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.busy.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.service.recordCareActivity(this.admissionUid, {
      feedingDone: !!raw.feedingDone,
      positionChanged: !!raw.positionChanged,
      bedBathDone: !!raw.bedBathDone,
      randomBloodSugarMmol: num(raw.randomBloodSugarMmol),
      fastingBloodSugarMmol: num(raw.fastingBloodSugarMmol),
      notes: raw.notes?.trim() || null
    }).pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (entry: CareActivityEntry) => this.activeModal.close(entry),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not record care activity.')
    });
  }
}

function num(v: number | string | null | undefined): number | null {
  if (v === null || v === undefined || v === '') return null;
  const n = Number(v);
  return Number.isFinite(n) ? n : null;
}
