import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { LabTestTypeService } from '../../masterdata/lab-tests/lab-test.service';
import { LabTestType } from '../../masterdata/lab-tests/lab-test.types';
import { LabBatchService } from './lab-batch.service';
import { BatchableOrder } from './lab-batch.types';

@Component({
  selector: 'app-lab-batch-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './lab-batch-create.component.html'
})
export class LabBatchCreateComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly labBatchService = inject(LabBatchService);
  private readonly labTestService = inject(LabTestTypeService);
  private readonly router = inject(Router);

  readonly labTests = signal<LabTestType[]>([]);
  readonly candidates = signal<BatchableOrder[]>([]);
  readonly selectedUids = signal<Set<string>>(new Set());
  readonly loadingCandidates = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly selectedCount = computed(() => this.selectedUids().size);

  readonly form = this.fb.nonNullable.group({
    labTestTypeUid: ['', [Validators.required]],
    note: ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    this.labTestService.search({ active: true, size: 500, sort: 'name,asc' }).subscribe({
      next: (res) => this.labTests.set(res.content),
      error: () => { /* dropdown stays empty; user sees validation error */ }
    });
  }

  /** Lab-test dropdown change — reload the eligible-orders picker. */
  onLabTestChange(): void {
    this.selectedUids.set(new Set());
    this.candidates.set([]);
    const uid = this.form.controls.labTestTypeUid.value;
    if (!uid) return;
    this.loadingCandidates.set(true);
    this.errorMessage.set(null);
    this.labBatchService.listBatchable(uid)
      .pipe(finalize(() => this.loadingCandidates.set(false)))
      .subscribe({
        next: (rows) => this.candidates.set(rows),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load eligible orders.')
      });
  }

  isSelected(uid: string): boolean { return this.selectedUids().has(uid); }

  toggle(uid: string): void {
    const next = new Set(this.selectedUids());
    if (next.has(uid)) next.delete(uid); else next.add(uid);
    this.selectedUids.set(next);
  }

  selectAll(): void { this.selectedUids.set(new Set(this.candidates().map((c) => c.orderUid))); }
  clearSelection(): void { this.selectedUids.set(new Set()); }

  submit(): void {
    if (this.submitting()) return;
    const orderUids = [...this.selectedUids()];
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    if (orderUids.length === 0) {
      this.errorMessage.set('Select at least one order to batch.');
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.labBatchService.create({
      labTestTypeUid: raw.labTestTypeUid,
      note: raw.note.trim() || null,
      orderUids
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (created) => void this.router.navigate(['/encounters/lab-batches', created.uid]),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not create batch.')
    });
  }

  cancel(): void {
    void this.router.navigate(['/encounters/lab-batches']);
  }
}
