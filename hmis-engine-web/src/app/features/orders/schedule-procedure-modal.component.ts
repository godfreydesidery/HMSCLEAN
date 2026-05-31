import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { ClinicalOrderService } from '../encounter/order/clinical-order.service';
import { ClinicalOrder } from '../encounter/order/clinical-order.types';
import { TheatreService } from '../masterdata/theatres/theatre.service';
import { Theatre } from '../masterdata/theatres/theatre.types';

/** Book a theatre + time slot for a PROCEDURE order. */
@Component({
  selector: 'app-schedule-procedure-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="modal-header">
      <h5 class="modal-title">Schedule procedure</h5>
      <button type="button" class="btn-close" (click)="activeModal.dismiss()" aria-label="Close"></button>
    </div>
    <div class="modal-body">
      @if (errorMessage()) {
        <div class="alert alert-danger d-flex align-items-center gap-2"><i class="bi bi-exclamation-circle"></i><span>{{ errorMessage() }}</span></div>
      }
      <form [formGroup]="form" class="row g-3">
        <div class="col-12">
          <label class="form-label" for="sch-theatre">Theatre <span class="text-danger">*</span></label>
          <select id="sch-theatre" class="form-select" formControlName="theatreUid">
            <option value="" disabled>{{ loadingTheatres() ? 'Loading…' : '— Select theatre —' }}</option>
            @for (t of theatres(); track t.uid) {
              <option [value]="t.uid">{{ t.name }}@if (t.location) { <span> · {{ t.location }}</span> }</option>
            }
          </select>
          @if (!loadingTheatres() && theatres().length === 0) {
            <div class="form-text text-warning-emphasis">No active theatres — add one under Master Data → Theatres.</div>
          }
        </div>
        <div class="col-12">
          <label class="form-label" for="sch-at">Scheduled date &amp; time <span class="text-danger">*</span></label>
          <input id="sch-at" type="datetime-local" class="form-control" formControlName="scheduledAt">
        </div>
      </form>
    </div>
    <div class="modal-footer">
      <button type="button" class="btn btn-light border" (click)="activeModal.dismiss()" [disabled]="submitting()">Cancel</button>
      <button type="button" class="btn btn-primary d-flex align-items-center gap-2" (click)="submit()" [disabled]="submitting() || form.invalid">
        @if (submitting()) { <output class="spinner-border spinner-border-sm" aria-live="polite"><span class="visually-hidden">Saving</span></output> }
        <i class="bi bi-calendar-check"></i>Schedule
      </button>
    </div>
  `
})
export class ScheduleProcedureModalComponent implements OnInit {
  @Input({ required: true }) orderUid!: string;

  private readonly fb = inject(FormBuilder);
  private readonly orderService = inject(ClinicalOrderService);
  private readonly theatreService = inject(TheatreService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly theatres = signal<Theatre[]>([]);
  readonly loadingTheatres = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    theatreUid: ['', [Validators.required]],
    scheduledAt: ['', [Validators.required]]
  });

  ngOnInit(): void {
    this.loadingTheatres.set(true);
    this.theatreService.search({ active: true, size: 200, sort: 'name,asc' })
      .pipe(finalize(() => this.loadingTheatres.set(false)))
      .subscribe({
        next: (page) => this.theatres.set(page.content),
        error: () => this.errorMessage.set('Could not load theatres.')
      });
  }

  submit(): void {
    if (this.submitting()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    // datetime-local is wall-clock with no zone; convert to an ISO instant.
    const scheduledAt = new Date(raw.scheduledAt).toISOString();
    this.orderService.schedule(this.orderUid, { theatreUid: raw.theatreUid, scheduledAt })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (order: ClinicalOrder) => this.activeModal.close(order),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not schedule the procedure.')
      });
  }
}
