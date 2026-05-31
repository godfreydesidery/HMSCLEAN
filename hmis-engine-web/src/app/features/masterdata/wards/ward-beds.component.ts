import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, finalize } from 'rxjs';

import { BedService } from './bed.service';
import { BED_STATUSES, Bed, BedStatus } from './bed.types';

/**
 * Per-ward bed management (opened as a modal from the ward list). Lists the ward's
 * beds with their live status, and lets staff add / edit / delete beds and flip a
 * bed out-of-service or back to free. Beds that are OCCUPIED or RESERVED (held for a
 * deposit-pending admission) cannot be freed/deleted/taken offline here — the
 * backend rejects it; the controls are disabled and the holding admission is shown.
 */
@Component({
  selector: 'app-ward-beds',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './ward-beds.component.html'
})
export class WardBedsComponent implements OnInit {
  @Input({ required: true }) wardUid!: string;
  @Input() wardName = '';

  private readonly fb = inject(FormBuilder);
  private readonly bedService = inject(BedService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly beds = signal<Bed[]>([]);
  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  /** Set while editing an existing bed; null when the form creates a new one. */
  readonly editing = signal<Bed | null>(null);

  readonly form = this.fb.nonNullable.group({
    label: ['', [Validators.required, Validators.maxLength(32)]],
    notes: ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void { this.load(); }

  private load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.bedService.listForWard(this.wardUid)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (rows) => this.beds.set(rows),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load beds.')
      });
  }

  startEdit(b: Bed): void {
    this.editing.set(b);
    this.form.setValue({ label: b.label, notes: b.notes ?? '' });
  }

  cancelEdit(): void {
    this.editing.set(null);
    this.form.reset({ label: '', notes: '' });
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const payload = { label: raw.label.trim(), notes: raw.notes?.trim() || null };
    const editing = this.editing();
    const req$ = editing
      ? this.bedService.update(editing.uid, payload)
      : this.bedService.create(this.wardUid, payload);
    req$.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: () => { this.cancelEdit(); this.load(); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save the bed.')
    });
  }

  outOfService(b: Bed): void {
    const reason = globalThis.prompt(`Take bed "${b.label}" out of service — reason (optional):`, '') ?? '';
    this.act(this.bedService.markOutOfService(b.uid, reason.trim() || null));
  }

  markFree(b: Bed): void { this.act(this.bedService.markFree(b.uid)); }
  toggleActive(b: Bed): void { this.act(this.bedService.setActive(b.uid, !b.active)); }

  delete(b: Bed): void {
    if (!globalThis.confirm(`Delete bed "${b.label}"? This cannot be undone.`)) return;
    this.act(this.bedService.delete(b.uid));
  }

  private act(req$: Observable<unknown>): void {
    this.errorMessage.set(null);
    req$.subscribe({
      next: () => this.load(),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'The action failed.')
    });
  }

  /** A bed physically in use (held or occupied) cannot be freed/deleted/taken offline. */
  inUse(b: Bed): boolean { return b.status === 'OCCUPIED' || b.status === 'RESERVED'; }

  badgeClass(s: BedStatus): string { return BED_STATUSES.find((x) => x.value === s)?.badgeClass ?? 'text-bg-light border'; }
  statusLabel(s: BedStatus): string { return BED_STATUSES.find((x) => x.value === s)?.label ?? s; }
}
