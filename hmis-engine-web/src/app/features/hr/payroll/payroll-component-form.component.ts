import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { PayrollComponentService } from './payroll-component.service';
import {
  BandRequest, CALC_BASES, CALC_METHODS, COMPONENT_TYPES, PayrollCalcBase, PayrollCalcMethod,
  PayrollComponent, PayrollComponentType
} from './payroll-component.types';

const AMOUNT = /^\d+(\.\d{1,2})?$/;
const PERCENT = /^\d+(\.\d{1,4})?$/;

@Component({
  selector: 'app-payroll-component-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './payroll-component-form.component.html'
})
export class PayrollComponentFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(PayrollComponentService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly types = COMPONENT_TYPES;
  readonly methods = CALC_METHODS;
  readonly bases = CALC_BASES;

  readonly editingUid = signal<string | null>(null);
  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.group({
    code:        this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(32)]),
    name:        this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(120)]),
    type:        this.fb.nonNullable.control<PayrollComponentType>('DEDUCTION'),
    method:      this.fb.nonNullable.control<PayrollCalcMethod>('FIXED'),
    base:        this.fb.nonNullable.control<PayrollCalcBase>('BASIC'),
    fixedAmount: this.fb.nonNullable.control('0.00', [Validators.pattern(AMOUNT)]),
    ratePercent: this.fb.nonNullable.control('0', [Validators.pattern(PERCENT)]),
    active:      this.fb.nonNullable.control(true),
    sortOrder:   this.fb.nonNullable.control(0, [Validators.min(0)]),
    bands:       this.fb.array<FormGroup>([])
  });

  readonly method = signal<PayrollCalcMethod>('FIXED');
  readonly isBand = computed(() => this.method() === 'BAND');
  readonly isPercent = computed(() => this.method() === 'PERCENT');
  readonly isFixed = computed(() => this.method() === 'FIXED');

  get bandsArray(): FormArray<FormGroup> { return this.form.controls.bands; }

  ngOnInit(): void {
    const uid = this.route.snapshot.paramMap.get('uid');
    if (uid) {
      this.editingUid.set(uid);
      this.loadForEdit(uid);
    }
  }

  onMethodChange(): void {
    const m = this.form.controls.method.value;
    this.method.set(m);
    if (m === 'BAND' && this.bandsArray.length === 0) this.addBand();
  }

  private loadForEdit(uid: string): void {
    this.loading.set(true);
    this.service.findByUid(uid).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (c) => this.patch(c),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load component.')
    });
  }

  private patch(c: PayrollComponent): void {
    this.form.patchValue({
      code: c.code,
      name: c.name,
      type: c.type,
      method: c.method,
      base: c.base,
      fixedAmount: c.fixedAmount ?? '0.00',
      ratePercent: c.percentRate ? toPercent(c.percentRate) : '0',
      active: c.active,
      sortOrder: c.sortOrder
    });
    this.form.controls.code.disable();
    this.method.set(c.method);
    this.bandsArray.clear();
    for (const b of c.bands) {
      this.bandsArray.push(this.newBand(b.fromAmount, b.toAmount, toPercent(b.rate)));
    }
  }

  newBand(fromAmount = '0.00', toAmount: string | null = '', ratePercent = '0'): FormGroup {
    return this.fb.group({
      fromAmount:  this.fb.nonNullable.control(fromAmount, [Validators.required, Validators.pattern(AMOUNT)]),
      toAmount:    this.fb.nonNullable.control(toAmount ?? '', [Validators.pattern(AMOUNT)]),
      ratePercent: this.fb.nonNullable.control(ratePercent, [Validators.required, Validators.pattern(PERCENT)])
    });
  }

  addBand(): void { this.bandsArray.push(this.newBand()); }
  removeBand(i: number): void { this.bandsArray.removeAt(i); }

  submit(): void {
    if (this.submitting()) return;
    this.errorMessage.set(null);
    const m = this.form.controls.method.value;

    // Method-specific validation.
    if (m === 'FIXED' && !AMOUNT.test(this.form.controls.fixedAmount.value)) {
      this.errorMessage.set('Enter a valid fixed amount.'); return;
    }
    if (m === 'PERCENT' && !PERCENT.test(this.form.controls.ratePercent.value)) {
      this.errorMessage.set('Enter a valid percentage.'); return;
    }
    if (m === 'BAND' && this.bandsArray.length === 0) {
      this.errorMessage.set('Add at least one band.'); return;
    }
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    const raw = this.form.getRawValue();
    const bands: BandRequest[] | undefined = m === 'BAND'
      ? this.bandsArray.controls.map((g) => {
          const v = g.getRawValue() as { fromAmount: string; toAmount: string; ratePercent: string };
          return {
            fromAmount: v.fromAmount,
            toAmount: v.toAmount?.trim() ? v.toAmount : null,
            rate: toFraction(v.ratePercent)
          };
        })
      : undefined;

    const fixedAmount = m === 'FIXED' ? raw.fixedAmount : null;
    const percentRate = m === 'PERCENT' ? toFraction(raw.ratePercent) : null;

    this.submitting.set(true);
    const uid = this.editingUid();
    const done = (c: PayrollComponent) => void this.router.navigate(['/hr/payroll/components']);
    const fail = (err: { error?: { message?: string } }) =>
      this.errorMessage.set(err?.error?.message ?? 'Could not save component.');

    if (uid) {
      this.service.update(uid, {
        name: raw.name, type: raw.type, method: raw.method, base: raw.base,
        fixedAmount, percentRate, active: raw.active, sortOrder: raw.sortOrder, bands
      }).pipe(finalize(() => this.submitting.set(false))).subscribe({ next: done, error: fail });
    } else {
      this.service.create({
        code: raw.code, name: raw.name, type: raw.type, method: raw.method, base: raw.base,
        fixedAmount, percentRate, active: raw.active, sortOrder: raw.sortOrder, bands
      }).pipe(finalize(() => this.submitting.set(false))).subscribe({ next: done, error: fail });
    }
  }

  cancel(): void { void this.router.navigate(['/hr/payroll/components']); }
}

/** Fraction string (0.1) → percent display string (10). */
function toPercent(fraction: string): string {
  const n = Number(fraction);
  return Number.isFinite(n) ? String(+(n * 100).toFixed(4)) : '0';
}

/** Percent input (10) → fraction string (0.1) for the backend. */
function toFraction(percent: string): string {
  const n = Number(percent);
  return Number.isFinite(n) ? String(+(n / 100).toFixed(6)) : '0';
}
