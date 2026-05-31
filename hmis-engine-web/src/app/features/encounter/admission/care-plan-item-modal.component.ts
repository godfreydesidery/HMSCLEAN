import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { NursingChartService } from './nursing-chart.service';
import { CarePlanItem } from './nursing-chart.types';

/** Author or edit a nursing care-plan item (problem / goal / intervention / evaluation). */
@Component({
  selector: 'app-care-plan-item-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="modal-header">
      <h5 class="modal-title">{{ existing ? 'Edit care-plan item' : 'New care-plan item' }}</h5>
      <button type="button" class="btn-close" (click)="activeModal.dismiss()" aria-label="Close"></button>
    </div>
    <div class="modal-body">
      @if (errorMessage()) {
        <div class="alert alert-danger d-flex align-items-center gap-2"><i class="bi bi-exclamation-circle"></i><span>{{ errorMessage() }}</span></div>
      }
      <form [formGroup]="form" class="row g-3">
        <div class="col-md-6">
          <label class="form-label" for="cp-problem">Problem</label>
          <textarea id="cp-problem" rows="2" class="form-control" formControlName="problem" maxlength="500"></textarea>
        </div>
        <div class="col-md-6">
          <label class="form-label" for="cp-goal">Goal</label>
          <textarea id="cp-goal" rows="2" class="form-control" formControlName="goal" maxlength="500"></textarea>
        </div>
        <div class="col-12">
          <label class="form-label" for="cp-intervention">Intervention</label>
          <textarea id="cp-intervention" rows="3" class="form-control" formControlName="intervention" maxlength="2000"></textarea>
        </div>
        <div class="col-12">
          <label class="form-label" for="cp-evaluation">Evaluation <span class="hmis-muted small">(optional)</span></label>
          <textarea id="cp-evaluation" rows="2" class="form-control" formControlName="evaluation" maxlength="2000"></textarea>
        </div>
      </form>
    </div>
    <div class="modal-footer">
      <button type="button" class="btn btn-light border" (click)="activeModal.dismiss()" [disabled]="busy()">Cancel</button>
      <button type="button" class="btn btn-primary d-flex align-items-center gap-2" (click)="submit()" [disabled]="busy() || form.invalid">
        @if (busy()) { <output class="spinner-border spinner-border-sm" aria-live="polite"><span class="visually-hidden">Saving</span></output> }
        <i class="bi bi-check2"></i>{{ existing ? 'Save' : 'Add' }}
      </button>
    </div>
  `
})
export class CarePlanItemModalComponent implements OnInit {
  @Input({ required: true }) admissionUid!: string;
  /** When set, the modal edits this item instead of creating a new one. */
  @Input() existing: CarePlanItem | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(NursingChartService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    problem:      ['', [Validators.required, Validators.maxLength(500)]],
    goal:         ['', [Validators.required, Validators.maxLength(500)]],
    intervention: ['', [Validators.required, Validators.maxLength(2000)]],
    evaluation:   ['', [Validators.maxLength(2000)]]
  });

  ngOnInit(): void {
    if (this.existing) {
      this.form.patchValue({
        problem: this.existing.problem,
        goal: this.existing.goal,
        intervention: this.existing.intervention,
        evaluation: this.existing.evaluation ?? ''
      });
    }
  }

  submit(): void {
    if (this.busy()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.busy.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const req = {
      problem: raw.problem.trim(),
      goal: raw.goal.trim(),
      intervention: raw.intervention.trim(),
      evaluation: raw.evaluation.trim() || null
    };
    const call = this.existing
      ? this.service.updateCarePlanItem(this.existing.uid, req)
      : this.service.addCarePlanItem(this.admissionUid, req);
    call.pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (item) => this.activeModal.close(item),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save care-plan item.')
    });
  }
}
