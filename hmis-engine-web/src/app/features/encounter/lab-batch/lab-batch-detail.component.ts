import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { LabBatchService } from './lab-batch.service';
import { BatchableOrder, LAB_BATCH_STATUSES, LabBatch, LabBatchStatus } from './lab-batch.types';

@Component({
  selector: 'app-lab-batch-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './lab-batch-detail.component.html'
})
export class LabBatchDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly labBatchService = inject(LabBatchService);

  readonly statuses = LAB_BATCH_STATUSES;
  readonly batch = signal<LabBatch | null>(null);
  readonly candidates = signal<BatchableOrder[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly selectedOrderUid = new FormControl('', { nonNullable: true, validators: [Validators.required] });

  readonly isOpen        = computed(() => this.batch()?.status === 'OPEN');
  readonly canProcess    = computed(() => this.batch()?.status === 'OPEN');
  readonly canComplete   = computed(() => {
    const s = this.batch()?.status;
    return s === 'OPEN' || s === 'PROCESSING';
  });
  readonly canCancel     = computed(() => {
    const s = this.batch()?.status;
    return s === 'OPEN' || s === 'PROCESSING';
  });

  constructor() {
    const uid = this.route.snapshot.paramMap.get('uid');
    if (!uid) {
      this.loading.set(false);
      this.errorMessage.set('Missing batch identifier.');
      return;
    }
    this.load(uid);
  }

  private load(uid: string): void {
    this.loading.set(true);
    this.labBatchService.findByUid(uid)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (b) => { this.batch.set(b); this.refreshCandidates(b); },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load batch.')
      });
  }

  /** Eligible (REQUESTED, unbatched) orders for this batch's test type — only while OPEN. */
  private refreshCandidates(b: LabBatch): void {
    if (b.status !== 'OPEN') { this.candidates.set([]); return; }
    this.labBatchService.listBatchable(b.labTestTypeUid).subscribe({
      next: (rows) => this.candidates.set(rows),
      error: () => this.candidates.set([])
    });
  }

  addOrder(): void {
    const current = this.batch();
    if (!current || this.busy()) return;
    if (this.selectedOrderUid.invalid) { this.selectedOrderUid.markAsTouched(); return; }
    const uid = this.selectedOrderUid.value;
    this.busy.set(true);
    this.errorMessage.set(null);
    this.labBatchService.addOrder(current.uid, uid)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: (b) => { this.batch.set(b); this.selectedOrderUid.reset(''); this.refreshCandidates(b); },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not add order to batch.')
      });
  }

  removeOrder(orderUid: string): void {
    const current = this.batch();
    if (!current || this.busy()) return;
    this.busy.set(true);
    this.errorMessage.set(null);
    this.labBatchService.removeOrder(current.uid, orderUid)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: (b) => { this.batch.set(b); this.refreshCandidates(b); },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not remove order.')
      });
  }

  markProcessing(): void { this.transition('process'); }
  markCompleted(): void { this.transition('complete'); }

  private transition(action: 'process' | 'complete'): void {
    const current = this.batch();
    if (!current || this.busy()) return;
    this.busy.set(true);
    this.errorMessage.set(null);
    const req$ = action === 'process'
      ? this.labBatchService.markProcessing(current.uid)
      : this.labBatchService.markCompleted(current.uid);
    req$.pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (b) => this.batch.set(b),
      error: (err) => this.errorMessage.set(err?.error?.message ?? `Could not ${action} batch.`)
    });
  }

  cancelBatch(): void {
    const current = this.batch();
    if (!current || this.busy()) return;
    const reason = globalThis.prompt('Reason for cancelling the batch? (optional)') ?? '';
    this.busy.set(true);
    this.errorMessage.set(null);
    this.labBatchService.cancel(current.uid, { reason: reason.trim() || null })
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: (b) => this.batch.set(b),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not cancel batch.')
      });
  }

  back(): void { void this.router.navigate(['/encounters/lab-batches']); }

  statusBadgeClass(s: LabBatchStatus): string {
    return 'badge ' + (this.statuses.find((x) => x.value === s)?.badgeClass ?? '');
  }
  statusLabel(s: LabBatchStatus): string {
    return this.statuses.find((x) => x.value === s)?.label ?? s;
  }
}
