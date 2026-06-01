import { CommonModule } from '@angular/common';
import { Component, Input, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { NursingChartService } from './nursing-chart.service';
import { FluidBalanceEntry } from './nursing-chart.types';

/** Record one intake/output reading on the admission's fluid-balance chart. */
@Component({
  selector: 'app-record-fluid-balance-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="modal-header">
      <h5 class="modal-title">Record fluid balance</h5>
      <button type="button" class="btn-close" (click)="activeModal.dismiss()" aria-label="Close"></button>
    </div>
    <div class="modal-body">
      @if (errorMessage()) {
        <div class="alert alert-danger d-flex align-items-center gap-2"><i class="bi bi-exclamation-circle"></i><span>{{ errorMessage() }}</span></div>
      }
      <form [formGroup]="form" class="row g-3">
        <div class="col-12 col-md-4">
          <label class="form-label" for="fb-intake">Intake (mL)</label>
          <input id="fb-intake" type="number" min="0" max="100000" class="form-control" formControlName="intakeMl">
        </div>
        <div class="col-12 col-md-4">
          <label class="form-label" for="fb-urine">Urine output (mL)</label>
          <input id="fb-urine" type="number" min="0" max="100000" class="form-control" formControlName="urineOutputMl">
        </div>
        <div class="col-12 col-md-4">
          <label class="form-label" for="fb-drain">Other output (mL)</label>
          <input id="fb-drain" type="number" min="0" max="100000" class="form-control" formControlName="drainageOutputMl">
        </div>
        <div class="col-12">
          <label class="form-label" for="fb-notes">Notes</label>
          <textarea id="fb-notes" rows="2" class="form-control" formControlName="notes" maxlength="500"></textarea>
        </div>
        <div class="col-12">
          <div class="alert alert-light border d-flex justify-content-between mb-0 py-2 px-3 small">
            <span class="hmis-muted">Net (intake − output)</span>
            <span class="fw-semibold" [class.text-danger]="net() < 0">{{ net() }} mL</span>
          </div>
        </div>
      </form>
    </div>
    <div class="modal-footer">
      <button type="button" class="btn btn-light border" (click)="activeModal.dismiss()" [disabled]="busy()">Cancel</button>
      <button type="button" class="btn btn-primary d-flex align-items-center gap-2" (click)="submit()" [disabled]="busy() || !hasAnyFigure()">
        @if (busy()) { <output class="spinner-border spinner-border-sm" aria-live="polite"><span class="visually-hidden">Saving</span></output> }
        <i class="bi bi-check2"></i>Record
      </button>
    </div>
  `
})
export class RecordFluidBalanceModalComponent {
  @Input({ required: true }) admissionUid!: string;

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(NursingChartService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.group({
    intakeMl:         [null as number | null, [Validators.min(0), Validators.max(100000)]],
    urineOutputMl:    [null as number | null, [Validators.min(0), Validators.max(100000)]],
    drainageOutputMl: [null as number | null, [Validators.min(0), Validators.max(100000)]],
    notes:            ['', [Validators.maxLength(500)]]
  });

  private readonly value = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });

  readonly hasAnyFigure = computed(() => {
    const v = this.value();
    return num(v.intakeMl) !== null || num(v.urineOutputMl) !== null || num(v.drainageOutputMl) !== null;
  });

  readonly net = computed(() => {
    const v = this.value();
    return (num(v.intakeMl) ?? 0) - ((num(v.urineOutputMl) ?? 0) + (num(v.drainageOutputMl) ?? 0));
  });

  submit(): void {
    if (this.busy() || !this.hasAnyFigure()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.busy.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.service.recordFluidBalance(this.admissionUid, {
      intakeMl: num(raw.intakeMl),
      urineOutputMl: num(raw.urineOutputMl),
      drainageOutputMl: num(raw.drainageOutputMl),
      notes: raw.notes?.trim() || null
    }).pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (entry: FluidBalanceEntry) => this.activeModal.close(entry),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not record fluid balance.')
    });
  }
}

function num(v: number | string | null | undefined): number | null {
  if (v === null || v === undefined || v === '') return null;
  const n = Number(v);
  return Number.isFinite(n) ? n : null;
}
