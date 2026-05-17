import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { ClinicalOrder, ClinicalOrderKind } from './clinical-order.types';
import { OrderResultService } from './order-result.service';
import {
  ORDER_RESULT_STATUSES, OrderResult, OrderResultStatus
} from './order-result.types';

@Component({
  selector: 'app-enter-result',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './enter-result.component.html'
})
export class EnterResultComponent implements OnInit {
  @Input({ required: true }) order!: ClinicalOrder;

  private readonly fb = inject(FormBuilder);
  private readonly resultService = inject(OrderResultService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly statuses = ORDER_RESULT_STATUSES;
  readonly result = signal<OrderResult | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    narrative: ['', [Validators.maxLength(8000)]],
    impression: ['', [Validators.maxLength(1000)]]
  });

  ngOnInit(): void {
    this.resultService.findForOrder(this.order.uid)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (r) => {
          this.result.set(r);
          if (r) {
            this.form.patchValue({ narrative: r.narrative ?? '', impression: r.impression ?? '' });
          }
        },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load result.')
      });
  }

  get kindLabel(): string {
    switch (this.order.kind) {
      case 'LAB_TEST':  return 'Lab result';
      case 'RADIOLOGY': return 'Radiology report';
      case 'PROCEDURE': return 'Procedure note';
    }
  }

  get narrativeLabel(): string {
    switch (this.order.kind) {
      case 'LAB_TEST':  return 'Findings / values';
      case 'RADIOLOGY': return 'Findings';
      case 'PROCEDURE': return 'Procedure notes';
    }
  }

  statusBadgeClass(s: OrderResultStatus): string {
    return 'badge ' + (this.statuses.find((x) => x.value === s)?.badgeClass ?? '');
  }
  statusLabel(s: OrderResultStatus): string {
    return this.statuses.find((x) => x.value === s)?.label ?? s;
  }

  get isFinalized(): boolean {
    const r = this.result();
    return r != null && r.status !== 'PRELIMINARY';
  }

  save(): void {
    if (this.busy()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const raw = this.form.getRawValue();
    const payload = {
      narrative: raw.narrative?.trim() || null,
      impression: raw.impression?.trim() || null
    };
    this.busy.set(true);
    this.errorMessage.set(null);
    const obs = this.isFinalized
      ? this.resultService.amend(this.order.uid, payload)
      : this.resultService.save(this.order.uid, payload);
    obs.pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (r) => { this.result.set(r); this.activeModal.close(r); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save result.')
    });
  }

  finalize(): void {
    if (this.busy()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.busy.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const payload = {
      narrative: raw.narrative?.trim() || null,
      impression: raw.impression?.trim() || null
    };
    // Persist current edits first, then finalize, so the finalized snapshot
    // matches what the user sees in the form.
    this.resultService.save(this.order.uid, payload).subscribe({
      next: () => this.resultService.finalize(this.order.uid).pipe(finalize(() => this.busy.set(false))).subscribe({
        next: (r) => { this.result.set(r); this.activeModal.close(r); },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not finalize.')
      }),
      error: (err) => { this.busy.set(false); this.errorMessage.set(err?.error?.message ?? 'Could not save result.'); }
    });
  }
}
