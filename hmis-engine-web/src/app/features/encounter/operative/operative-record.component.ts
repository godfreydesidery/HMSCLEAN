import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';

import { OperativeRecordService } from './operative-record.service';
import {
  CreateAmendmentRequest, OperativeRecord, OperativeRecordAmendment, UpsertOperativeRecordRequest
} from './operative-record.types';

/**
 * Structured operative record for a PROCEDURE clinical order. While UNLOCKED the
 * editable fields are an editable form (Save = PUT upsert). Locking (POST /lock)
 * makes it read-only; from then on changes are appended as reason-stamped
 * amendments (POST /amendments).
 */
@Component({
  selector: 'app-operative-record',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './operative-record.component.html'
})
export class OperativeRecordComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(OperativeRecordService);

  readonly orderUid = this.route.snapshot.paramMap.get('orderUid') ?? '';

  readonly record = signal<OperativeRecord | null>(null);
  readonly amendments = signal<OperativeRecordAmendment[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly isLocked = computed(() => this.record()?.lockedAt != null);
  readonly hasRecord = computed(() => this.record() != null);

  /** Editable fields (UpsertOperativeRecordRequest). Long fields render as textareas. */
  readonly form = this.fb.nonNullable.group({
    findings: ['', [Validators.maxLength(4000)]],
    technique: ['', [Validators.maxLength(4000)]],
    instruments: ['', [Validators.maxLength(2000)]],
    complications: ['', [Validators.maxLength(2000)]],
    specimens: ['', [Validators.maxLength(2000)]],
    surgeonUsername: ['', [Validators.maxLength(64)]],
    assistants: ['', [Validators.maxLength(500)]],
    anaesthetistUsername: ['', [Validators.maxLength(64)]],
    anaesthesiaType: ['', [Validators.maxLength(64)]],
    scrubNurse: ['', [Validators.maxLength(120)]],
    circulatingNurse: ['', [Validators.maxLength(120)]],
    startedAt: [''],
    endedAt: ['']
  });

  /** New amendment (reason required). */
  readonly amendForm = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(1000)]],
    findings: ['', [Validators.maxLength(4000)]],
    technique: ['', [Validators.maxLength(4000)]],
    instruments: ['', [Validators.maxLength(2000)]],
    complications: ['', [Validators.maxLength(2000)]],
    specimens: ['', [Validators.maxLength(2000)]],
    assistants: ['', [Validators.maxLength(500)]],
    anaesthesiaType: ['', [Validators.maxLength(64)]],
    scrubNurse: ['', [Validators.maxLength(120)]],
    circulatingNurse: ['', [Validators.maxLength(120)]]
  });

  constructor() {
    if (!this.orderUid) {
      this.loading.set(false);
      this.errorMessage.set('Missing order identifier.');
      return;
    }
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.service.find(this.orderUid)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (r) => {
          this.record.set(r);
          if (r) this.patch(r);
          if (r?.lockedAt != null) this.loadAmendments();
        },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load operative record.')
      });
  }

  private loadAmendments(): void {
    this.service.listAmendments(this.orderUid).subscribe({
      next: (rows) => this.amendments.set(rows),
      error: () => this.amendments.set([])
    });
  }

  /** Fill the editable form from a loaded record. Instant -> datetime-local (first 16 chars). */
  private patch(r: OperativeRecord): void {
    this.form.patchValue({
      findings: r.findings ?? '',
      technique: r.technique ?? '',
      instruments: r.instruments ?? '',
      complications: r.complications ?? '',
      specimens: r.specimens ?? '',
      surgeonUsername: r.surgeonUsername ?? '',
      assistants: r.assistants ?? '',
      anaesthetistUsername: r.anaesthetistUsername ?? '',
      anaesthesiaType: r.anaesthesiaType ?? '',
      scrubNurse: r.scrubNurse ?? '',
      circulatingNurse: r.circulatingNurse ?? '',
      startedAt: r.startedAt ? r.startedAt.substring(0, 16) : '',
      endedAt: r.endedAt ? r.endedAt.substring(0, 16) : ''
    });
  }

  private upsertPayload(): UpsertOperativeRecordRequest {
    const r = this.form.getRawValue();
    return {
      findings: r.findings.trim() || null,
      technique: r.technique.trim() || null,
      instruments: r.instruments.trim() || null,
      complications: r.complications.trim() || null,
      specimens: r.specimens.trim() || null,
      surgeonUsername: r.surgeonUsername.trim() || null,
      assistants: r.assistants.trim() || null,
      anaesthetistUsername: r.anaesthetistUsername.trim() || null,
      anaesthesiaType: r.anaesthesiaType.trim() || null,
      scrubNurse: r.scrubNurse.trim() || null,
      circulatingNurse: r.circulatingNurse.trim() || null,
      startedAt: r.startedAt ? new Date(r.startedAt).toISOString() : null,
      endedAt: r.endedAt ? new Date(r.endedAt).toISOString() : null
    };
  }

  save(): void {
    if (this.busy() || this.isLocked()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.busy.set(true);
    this.errorMessage.set(null);
    this.service.upsert(this.orderUid, this.upsertPayload())
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: (r) => { this.record.set(r); this.patch(r); },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save operative record.')
      });
  }

  lock(): void {
    if (this.busy() || this.isLocked() || !this.hasRecord()) return;
    if (!globalThis.confirm('Lock this operative record? Once locked it becomes read-only and further changes must be made as amendments.')) return;
    this.busy.set(true);
    this.errorMessage.set(null);
    this.service.lock(this.orderUid)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: (r) => { this.record.set(r); this.loadAmendments(); },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not lock operative record.')
      });
  }

  private amendPayload(): CreateAmendmentRequest {
    const r = this.amendForm.getRawValue();
    return {
      reason: r.reason.trim(),
      findings: r.findings.trim() || null,
      technique: r.technique.trim() || null,
      instruments: r.instruments.trim() || null,
      complications: r.complications.trim() || null,
      specimens: r.specimens.trim() || null,
      assistants: r.assistants.trim() || null,
      anaesthesiaType: r.anaesthesiaType.trim() || null,
      scrubNurse: r.scrubNurse.trim() || null,
      circulatingNurse: r.circulatingNurse.trim() || null
    };
  }

  addAmendment(): void {
    if (this.busy()) return;
    if (this.amendForm.invalid) { this.amendForm.markAllAsTouched(); return; }
    this.busy.set(true);
    this.errorMessage.set(null);
    this.service.addAmendment(this.orderUid, this.amendPayload())
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: (a) => { this.amendments.set([...this.amendments(), a]); this.amendForm.reset(); },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not add amendment.')
      });
  }

  back(): void { void this.router.navigate(['/orders']); }
}
