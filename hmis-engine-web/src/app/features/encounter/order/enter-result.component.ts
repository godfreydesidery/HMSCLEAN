import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize, forkJoin, of } from 'rxjs';

import { ClinicalOrder } from './clinical-order.types';
import { OrderResultService } from './order-result.service';
import {
  AnalyteTemplate, LAB_RESULT_FLAGS, LabResultFlag, LabResultLine, LabResultLineInput,
  ORDER_RESULT_STATUSES, OrderResult, OrderResultStatus, SaveResultRequest
} from './order-result.types';

@Component({
  selector: 'app-enter-result',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './enter-result.component.html'
})
export class EnterResultComponent implements OnInit {
  order!: ClinicalOrder;

  private readonly fb = inject(FormBuilder);
  private readonly resultService = inject(OrderResultService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly statuses = ORDER_RESULT_STATUSES;
  readonly result = signal<OrderResult | null>(null);
  readonly analytes = signal<AnalyteTemplate[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.group({
    narrative: this.fb.nonNullable.control('', [Validators.maxLength(8000)]),
    impression: this.fb.nonNullable.control('', [Validators.maxLength(1000)]),
    lines: this.fb.array([] as FormGroup[])
  });

  /** Saved lines keyed by analyte uid — drives the flag + reference columns. */
  private readonly savedLineByAnalyte = computed(() => {
    const map = new Map<string, LabResultLine>();
    for (const l of this.result()?.lines ?? []) map.set(l.analyteUid, l);
    return map;
  });

  get isLab(): boolean { return this.order.kind === 'LAB_TEST'; }
  get linesArray(): FormArray<FormGroup> { return this.form.get('lines') as FormArray<FormGroup>; }

  ngOnInit(): void {
    forkJoin({
      result: this.resultService.findForOrder(this.order.uid),
      template: this.isLab ? this.resultService.template(this.order.uid) : of([] as AnalyteTemplate[])
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ result, template }) => {
          this.result.set(result);
          if (result) {
            this.form.patchValue({ narrative: result.narrative ?? '', impression: result.impression ?? '' });
          }
          this.analytes.set([...template].sort((a, b) => a.displayOrder - b.displayOrder || a.code.localeCompare(b.code)));
          this.buildLines(result?.lines ?? []);
        },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load result.')
      });
  }

  private buildLines(existing: LabResultLine[]): void {
    const byAnalyte = new Map(existing.map((l) => [l.analyteUid, l]));
    this.linesArray.clear();
    for (const a of this.analytes()) {
      const prev = byAnalyte.get(a.uid);
      this.linesArray.push(this.fb.group({
        analyteUid: this.fb.nonNullable.control(a.uid),
        valueNumeric: this.fb.control<number | null>(prev?.valueNumeric ?? null),
        valueText: this.fb.nonNullable.control(prev?.valueText ?? '')
      }));
    }
  }

  // ----- display helpers --------------------------------------------------

  get kindLabel(): string {
    switch (this.order.kind) {
      case 'LAB_TEST':  return 'Lab result';
      case 'RADIOLOGY': return 'Radiology report';
      case 'PROCEDURE': return 'Procedure note';
    }
  }
  get narrativeLabel(): string {
    switch (this.order.kind) {
      case 'LAB_TEST':  return 'Comments / microscopy notes';
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

  savedLine(analyteUid: string): LabResultLine | undefined { return this.savedLineByAnalyte().get(analyteUid); }
  flagBadgeClass(flag: LabResultFlag): string { return 'badge ' + LAB_RESULT_FLAGS[flag].badgeClass; }
  flagLabel(flag: LabResultFlag): string { return LAB_RESULT_FLAGS[flag].label; }

  get isFinalized(): boolean {
    const r = this.result();
    return r != null && r.status !== 'PRELIMINARY';
  }

  // ----- actions ----------------------------------------------------------

  private buildPayload(): SaveResultRequest {
    const raw = this.form.getRawValue();
    return {
      narrative: raw.narrative?.trim() || null,
      impression: raw.impression?.trim() || null,
      lines: this.isLab ? this.linePayload() : null
    };
  }

  private linePayload(): LabResultLineInput[] {
    return this.linesArray.controls.map((g) => {
      const num = g.get('valueNumeric')!.value as number | null;
      const text = (g.get('valueText')!.value as string)?.trim() || null;
      return {
        analyteUid: g.get('analyteUid')!.value as string,
        valueNumeric: num ?? null,
        valueText: text
      };
    });
  }

  save(): void {
    if (this.busy()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.busy.set(true);
    this.errorMessage.set(null);
    const payload = this.buildPayload();
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
    const payload = this.buildPayload();
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
