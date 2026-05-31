import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { LabAnalyteService } from './lab-analyte.service';
import {
  ANALYTE_VALUE_KINDS, AnalyteValueKind, LabReferenceRange, LabTestAnalyte, RANGE_SEXES, RangeSex
} from './lab-analyte.types';
import { LabTestType } from './lab-test.types';

@Component({
  selector: 'app-lab-analytes',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './lab-analytes.component.html'
})
export class LabAnalytesComponent implements OnInit {
  labTest!: LabTestType;

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(LabAnalyteService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly valueKinds = ANALYTE_VALUE_KINDS;
  readonly sexes = RANGE_SEXES;

  readonly analytes = signal<LabTestAnalyte[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);

  // analyte add/edit
  readonly showAnalyteForm = signal(false);
  readonly editingAnalyteUid = signal<string | null>(null);
  readonly analyteForm = this.fb.group({
    code: ['', [Validators.required, Validators.maxLength(32), Validators.pattern(/^[A-Za-z0-9._-]+$/)]],
    name: ['', [Validators.required, Validators.maxLength(120)]],
    unit: ['', [Validators.maxLength(32)]],
    valueKind: ['NUMERIC' as AnalyteValueKind, [Validators.required]],
    displayOrder: [0, [Validators.required, Validators.min(0)]]
  });

  // range add/edit
  readonly expanded = signal<string | null>(null);
  readonly rangeFormAnalyteUid = signal<string | null>(null);
  readonly editingRangeUid = signal<string | null>(null);
  readonly rangeForm = this.fb.group({
    sex: ['ANY' as RangeSex, [Validators.required]],
    ageMinDays: this.fb.control<number | null>(null, [Validators.min(0)]),
    ageMaxDays: this.fb.control<number | null>(null, [Validators.min(0)]),
    refLow: this.fb.control<number | null>(null),
    refHigh: this.fb.control<number | null>(null),
    criticalLow: this.fb.control<number | null>(null),
    criticalHigh: this.fb.control<number | null>(null),
    normalText: ['', [Validators.maxLength(200)]],
    rangeDisplay: ['', [Validators.maxLength(120)]]
  });

  ngOnInit(): void { this.reload(); }

  private reload(): void {
    this.loading.set(true);
    this.service.listAnalytes(this.labTest.uid)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (a) => this.analytes.set(a),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load analytes.')
      });
  }

  // ----- analyte form -----------------------------------------------------

  startAddAnalyte(): void {
    this.editingAnalyteUid.set(null);
    this.analyteForm.reset({ code: '', name: '', unit: '', valueKind: 'NUMERIC', displayOrder: this.analytes().length + 1 });
    this.analyteForm.get('code')!.enable();
    this.showAnalyteForm.set(true);
  }

  startEditAnalyte(a: LabTestAnalyte): void {
    this.editingAnalyteUid.set(a.uid);
    this.analyteForm.reset({
      code: a.code, name: a.name, unit: a.unit ?? '', valueKind: a.valueKind, displayOrder: a.displayOrder
    });
    this.analyteForm.get('code')!.disable(); // code is immutable once created
    this.showAnalyteForm.set(true);
  }

  cancelAnalyteForm(): void { this.showAnalyteForm.set(false); this.editingAnalyteUid.set(null); }

  submitAnalyte(): void {
    if (this.busy()) return;
    if (this.analyteForm.invalid) { this.analyteForm.markAllAsTouched(); return; }
    const v = this.analyteForm.getRawValue();
    this.busy.set(true);
    this.errorMessage.set(null);
    const editingUid = this.editingAnalyteUid();
    const done = () => { this.busy.set(false); this.showAnalyteForm.set(false); this.editingAnalyteUid.set(null); this.reload(); };
    const fail = (err: unknown) => { this.busy.set(false); this.errorMessage.set(errMsg(err, 'Could not save analyte.')); };
    if (editingUid) {
      this.service.updateAnalyte(editingUid, {
        name: v.name!.trim(), unit: trimOrNull(v.unit), valueKind: v.valueKind!, displayOrder: v.displayOrder!,
        active: this.analytes().find((x) => x.uid === editingUid)?.active ?? true
      }).subscribe({ next: done, error: fail });
    } else {
      this.service.createAnalyte(this.labTest.uid, {
        code: v.code!.trim().toUpperCase(), name: v.name!.trim(), unit: trimOrNull(v.unit),
        valueKind: v.valueKind!, displayOrder: v.displayOrder!
      }).subscribe({ next: done, error: fail });
    }
  }

  toggleActive(a: LabTestAnalyte): void {
    this.busy.set(true);
    this.service.updateAnalyte(a.uid, {
      name: a.name, unit: a.unit, valueKind: a.valueKind, displayOrder: a.displayOrder, active: !a.active
    }).pipe(finalize(() => this.busy.set(false)))
      .subscribe({ next: () => this.reload(), error: (err) => this.errorMessage.set(errMsg(err, 'Could not update analyte.')) });
  }

  deleteAnalyte(a: LabTestAnalyte): void {
    if (!globalThis.confirm(`Delete analyte "${a.name}" and its reference ranges?`)) return;
    this.busy.set(true);
    this.service.deleteAnalyte(a.uid).pipe(finalize(() => this.busy.set(false)))
      .subscribe({ next: () => this.reload(), error: (err) => this.errorMessage.set(errMsg(err, 'Could not delete analyte.')) });
  }

  // ----- ranges -----------------------------------------------------------

  toggleRanges(a: LabTestAnalyte): void {
    this.expanded.set(this.expanded() === a.uid ? null : a.uid);
    this.cancelRangeForm();
  }

  startAddRange(a: LabTestAnalyte): void {
    this.editingRangeUid.set(null);
    this.rangeFormAnalyteUid.set(a.uid);
    this.rangeForm.reset({ sex: 'ANY', ageMinDays: null, ageMaxDays: null, refLow: null, refHigh: null,
      criticalLow: null, criticalHigh: null, normalText: '', rangeDisplay: '' });
  }

  startEditRange(a: LabTestAnalyte, r: LabReferenceRange): void {
    this.editingRangeUid.set(r.uid);
    this.rangeFormAnalyteUid.set(a.uid);
    this.rangeForm.reset({
      sex: r.sex, ageMinDays: r.ageMinDays, ageMaxDays: r.ageMaxDays, refLow: r.refLow, refHigh: r.refHigh,
      criticalLow: r.criticalLow, criticalHigh: r.criticalHigh, normalText: r.normalText ?? '', rangeDisplay: r.rangeDisplay ?? ''
    });
  }

  cancelRangeForm(): void { this.rangeFormAnalyteUid.set(null); this.editingRangeUid.set(null); }

  submitRange(): void {
    if (this.busy()) return;
    const analyteUid = this.rangeFormAnalyteUid();
    if (!analyteUid || this.rangeForm.invalid) { this.rangeForm.markAllAsTouched(); return; }
    const v = this.rangeForm.getRawValue();
    const body = {
      sex: v.sex!, ageMinDays: v.ageMinDays ?? null, ageMaxDays: v.ageMaxDays ?? null,
      refLow: v.refLow ?? null, refHigh: v.refHigh ?? null, criticalLow: v.criticalLow ?? null, criticalHigh: v.criticalHigh ?? null,
      normalText: trimOrNull(v.normalText), rangeDisplay: trimOrNull(v.rangeDisplay)
    };
    this.busy.set(true);
    this.errorMessage.set(null);
    const done = () => { this.busy.set(false); this.cancelRangeForm(); this.reload(); };
    const fail = (err: unknown) => { this.busy.set(false); this.errorMessage.set(errMsg(err, 'Could not save reference range.')); };
    const editingUid = this.editingRangeUid();
    if (editingUid) {
      const current = this.analytes().flatMap((a) => a.ranges).find((r) => r.uid === editingUid);
      this.service.updateRange(editingUid, { ...body, active: current?.active ?? true }).subscribe({ next: done, error: fail });
    } else {
      this.service.addRange(analyteUid, body).subscribe({ next: done, error: fail });
    }
  }

  deleteRange(r: LabReferenceRange): void {
    if (!globalThis.confirm('Delete this reference range?')) return;
    this.busy.set(true);
    this.service.deleteRange(r.uid).pipe(finalize(() => this.busy.set(false)))
      .subscribe({ next: () => this.reload(), error: (err) => this.errorMessage.set(errMsg(err, 'Could not delete range.')) });
  }

  // ----- display ----------------------------------------------------------

  valueKindLabel(k: AnalyteValueKind): string { return this.valueKinds.find((x) => x.value === k)?.label ?? k; }

  ageBand(r: LabReferenceRange): string {
    if (r.ageMinDays == null && r.ageMaxDays == null) return 'all ages';
    const lo = r.ageMinDays == null ? '0' : String(r.ageMinDays);
    const hi = r.ageMaxDays == null ? '∞' : String(r.ageMaxDays);
    return `${lo}–${hi} d`;
  }
  refSummary(r: LabReferenceRange): string {
    if (r.rangeDisplay) return r.rangeDisplay;
    if (r.refLow != null || r.refHigh != null) return `${r.refLow ?? '–'} … ${r.refHigh ?? '–'}`;
    if (r.normalText) return r.normalText;
    return '—';
  }
}

function trimOrNull(s: string | null | undefined): string | null {
  const t = (s ?? '').trim();
  return t === '' ? null : t;
}
function errMsg(err: unknown, fallback: string): string {
  const e = err as { error?: { message?: string } };
  return e?.error?.message ?? fallback;
}
