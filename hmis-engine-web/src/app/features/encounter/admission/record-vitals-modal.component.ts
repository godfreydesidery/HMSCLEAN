import { CommonModule } from '@angular/common';
import { Component, Input, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { NursingChartService } from './nursing-chart.service';
import { VitalsEntry } from './nursing-chart.types';

/** Record one set of inpatient observations against an admission. */
@Component({
  selector: 'app-record-vitals-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="modal-header">
      <h5 class="modal-title">Record vitals</h5>
      <button type="button" class="btn-close" (click)="activeModal.dismiss()" aria-label="Close"></button>
    </div>
    <div class="modal-body">
      @if (errorMessage()) {
        <div class="alert alert-danger d-flex align-items-center gap-2"><i class="bi bi-exclamation-circle"></i><span>{{ errorMessage() }}</span></div>
      }
      <form [formGroup]="form" class="row g-3">
        <div class="col-6 col-md-4">
          <label class="form-label" for="v-temp">Temperature (°C)</label>
          <input id="v-temp" type="number" step="0.1" class="form-control" formControlName="temperatureC">
        </div>
        <div class="col-6 col-md-4">
          <label class="form-label" for="v-pulse">Pulse (bpm)</label>
          <input id="v-pulse" type="number" class="form-control" formControlName="pulseBpm">
        </div>
        <div class="col-6 col-md-4">
          <label class="form-label" for="v-resp">Respirations</label>
          <input id="v-resp" type="number" class="form-control" formControlName="respirationsBpm">
        </div>
        <div class="col-6 col-md-4">
          <label class="form-label" for="v-sys">Systolic BP</label>
          <input id="v-sys" type="number" class="form-control" formControlName="systolicBp">
        </div>
        <div class="col-6 col-md-4">
          <label class="form-label" for="v-dia">Diastolic BP</label>
          <input id="v-dia" type="number" class="form-control" formControlName="diastolicBp">
        </div>
        <div class="col-6 col-md-4">
          <label class="form-label" for="v-spo2">SpO₂ (%)</label>
          <input id="v-spo2" type="number" class="form-control" formControlName="spo2Percent">
        </div>
        <div class="col-6 col-md-4">
          <label class="form-label" for="v-glucose">Blood glucose (mmol/L)</label>
          <input id="v-glucose" type="number" step="0.1" class="form-control" formControlName="bloodGlucoseMmol">
        </div>
        <div class="col-6 col-md-4">
          <label class="form-label" for="v-pain">Pain score (0–10)</label>
          <input id="v-pain" type="number" class="form-control" formControlName="painScore">
        </div>
        <div class="col-12">
          <label class="form-label" for="v-notes">Notes</label>
          <textarea id="v-notes" rows="2" class="form-control" formControlName="notes" maxlength="500"></textarea>
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
export class RecordVitalsModalComponent {
  @Input({ required: true }) admissionUid!: string;

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(NursingChartService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    temperatureC:     [null as number | null],
    pulseBpm:         [null as number | null, [Validators.min(0), Validators.max(300)]],
    respirationsBpm:  [null as number | null, [Validators.min(0), Validators.max(120)]],
    systolicBp:       [null as number | null, [Validators.min(0), Validators.max(300)]],
    diastolicBp:      [null as number | null, [Validators.min(0), Validators.max(300)]],
    spo2Percent:      [null as number | null, [Validators.min(0), Validators.max(100)]],
    bloodGlucoseMmol: [null as number | null, [Validators.min(0)]],
    painScore:        [null as number | null, [Validators.min(0), Validators.max(10)]],
    notes:            ['', [Validators.maxLength(500)]]
  });

  submit(): void {
    if (this.busy()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.busy.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.service.recordVitals(this.admissionUid, {
      temperatureC: raw.temperatureC,
      pulseBpm: raw.pulseBpm,
      respirationsBpm: raw.respirationsBpm,
      systolicBp: raw.systolicBp,
      diastolicBp: raw.diastolicBp,
      spo2Percent: raw.spo2Percent,
      bloodGlucoseMmol: raw.bloodGlucoseMmol,
      painScore: raw.painScore,
      notes: raw.notes.trim() || null
    }).pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (entry: VitalsEntry) => this.activeModal.close(entry),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not record vitals.')
    });
  }
}
