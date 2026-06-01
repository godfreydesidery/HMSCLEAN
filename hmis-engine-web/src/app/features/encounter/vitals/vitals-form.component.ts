import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { VitalsService } from './vitals.service';
import { PatientVitals, VitalsStatus } from './vitals.types';

/**
 * Outpatient nurse vitals capture (OPC-3). Opens for one consultation: the nurse
 * fills the readings (save → PENDING, editable) then submits (→ SUBMITTED, locked).
 * Once SUBMITTED the form is read-only and only a doctor can consume it (elsewhere).
 */
@Component({
  selector: 'app-vitals-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './vitals-form.component.html'
})
export class VitalsFormComponent implements OnInit {
  @Input({ required: true }) consultationUid!: string;

  private readonly fb = inject(FormBuilder);
  private readonly vitalsService = inject(VitalsService);
  protected readonly activeModal = inject(NgbActiveModal);

  /** The open vitals row for this consultation (loaded on init), if any. */
  readonly current = signal<PatientVitals | null>(null);
  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  /** The current lifecycle status — EMPTY until the nurse first saves. */
  readonly status = computed<VitalsStatus>(() => this.current()?.status ?? 'EMPTY');
  /** Once SUBMITTED (or ARCHIVED) the set is locked — read-only to the nurse. */
  readonly locked = computed(() => {
    const s = this.status();
    return s === 'SUBMITTED' || s === 'ARCHIVED';
  });
  /** A saved PENDING row exists, so it can be submitted. */
  readonly canSubmit = computed(() => this.status() === 'PENDING' && this.current()?.uid != null);

  readonly form = this.fb.group({
    temperatureC:           [null as number | null, [Validators.min(25), Validators.max(45)]],
    pulseBpm:               [null as number | null, [Validators.min(20), Validators.max(250)]],
    respirationBpm:         [null as number | null, [Validators.min(5),  Validators.max(80)]],
    bloodPressureSystolic:  [null as number | null, [Validators.min(40), Validators.max(260)]],
    bloodPressureDiastolic: [null as number | null, [Validators.min(20), Validators.max(200)]],
    spo2Percent:            [null as number | null, [Validators.min(40), Validators.max(100)]],
    weightKg:               [null as number | null, [Validators.min(0.5), Validators.max(400)]],
    heightCm:               [null as number | null, [Validators.min(20), Validators.max(260)]],
    notes:                  ['' as string,           [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    this.vitalsService.list(this.consultationUid)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (rows) => {
          // The open row is the latest non-archived (PENDING/SUBMITTED) one, else the latest.
          const open = rows.find((r) => r.status === 'PENDING' || r.status === 'SUBMITTED') ?? rows[0] ?? null;
          this.applyRow(open);
        },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load vitals.')
      });
  }

  private applyRow(row: PatientVitals | null): void {
    this.current.set(row);
    if (row) {
      this.form.patchValue({
        temperatureC: row.temperatureC,
        pulseBpm: row.pulseBpm,
        respirationBpm: row.respirationBpm,
        bloodPressureSystolic: row.bloodPressureSystolic,
        bloodPressureDiastolic: row.bloodPressureDiastolic,
        spo2Percent: row.spo2Percent,
        weightKg: row.weightKg,
        heightCm: row.heightCm,
        notes: row.notes ?? ''
      }, { emitEvent: false });
    }
    if (this.locked()) this.form.disable({ emitEvent: false });
  }

  /** Nurse save — persists the readings as PENDING (still editable). */
  save(): void {
    if (this.locked() || this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const v = this.form.value;
    this.vitalsService.save(this.consultationUid, {
      temperatureC: v.temperatureC ?? null,
      pulseBpm: v.pulseBpm ?? null,
      respirationBpm: v.respirationBpm ?? null,
      bloodPressureSystolic: v.bloodPressureSystolic ?? null,
      bloodPressureDiastolic: v.bloodPressureDiastolic ?? null,
      spo2Percent: v.spo2Percent ?? null,
      weightKg: v.weightKg ?? null,
      heightCm: v.heightCm ?? null,
      notes: (v.notes ?? '').trim() || null
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (vitals) => this.current.set(vitals),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save vitals.')
    });
  }

  /** Nurse submit — locks the saved set (PENDING → SUBMITTED) and closes. */
  submit(): void {
    const row = this.current();
    if (!this.canSubmit() || !row || this.submitting()) return;
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.vitalsService.submit(this.consultationUid, row.uid)
      .pipe(finalize(() => this.submitting.set(false))).subscribe({
        next: (vitals) => this.activeModal.close(vitals),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not submit vitals.')
      });
  }

  done(): void { this.activeModal.close(this.current() ?? undefined); }
}
