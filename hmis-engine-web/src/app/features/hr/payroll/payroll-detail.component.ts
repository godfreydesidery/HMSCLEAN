import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { EmployeeService } from '../employee/employee.service';
import { PayrollComponentService } from './payroll-component.service';
import { ComputedPayroll } from './payroll-component.types';
import { PayrollService } from './payroll.service';
import {
  EmployeeSummary, PAYROLL_PERIOD_STATUSES, PayrollItem, PayrollPeriod, PayrollPeriodStatus
} from './payroll.types';

@Component({
  selector: 'app-payroll-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './payroll-detail.component.html'
})
export class PayrollDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly payrollService = inject(PayrollService);
  private readonly employeeService = inject(EmployeeService);
  private readonly componentService = inject(PayrollComponentService);

  readonly statuses = PAYROLL_PERIOD_STATUSES;

  readonly period = signal<PayrollPeriod | null>(null);
  readonly items = signal<PayrollItem[]>([]);
  readonly employees = signal<EmployeeSummary[]>([]);

  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);

  // Auto-prefill from configurable components.
  readonly computed = signal<ComputedPayroll | null>(null);
  readonly computing = signal(false);
  readonly computeForm = this.fb.nonNullable.group({
    basicSalary: ['0.00', [Validators.required, Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
    workedDays:  [null as number | null],
    periodDays:  [null as number | null]
  });

  readonly isDraft = computed(() => this.period()?.status === 'DRAFT');
  readonly canVerify = computed(() => this.period()?.status === 'DRAFT');
  readonly canApprove = computed(() => this.period()?.status === 'VERIFIED');
  readonly canPay = computed(() => this.period()?.status === 'APPROVED');
  readonly canCancel = computed(() => {
    const s = this.period()?.status;
    return s === 'DRAFT' || s === 'VERIFIED' || s === 'APPROVED';
  });

  readonly itemForm = this.fb.nonNullable.group({
    employeeUid:      ['', [Validators.required]],
    grossPay:         ['0.00', [Validators.required, Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
    totalDeductions:  ['0.00', [Validators.required, Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
    paymentMethod:    [''],
    paymentReference: [''],
    note:             ['']
  });

  ngOnInit(): void {
    const uid = this.route.snapshot.paramMap.get('uid');
    if (!uid) { this.errorMessage.set('Missing period uid.'); this.loading.set(false); return; }
    this.load(uid);
    this.employeeService.search({ size: 500, sort: 'lastName,asc' }).subscribe({
      next: (page) => this.employees.set(page.content),
      error: () => { /* empty dropdown — service errors surface elsewhere */ }
    });
  }

  private load(uid: string): void {
    this.loading.set(true);
    this.payrollService.findByUid(uid)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (res) => {
          this.period.set(res.period);
          this.items.set(res.items);
          this.computeForm.controls.periodDays.setValue(periodLengthDays(res.period));
        },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load period.')
      });
  }

  /** Compute gross + deductions from the active components and prefill the item form. */
  computePrefill(): void {
    if (this.computing()) return;
    if (this.computeForm.controls.basicSalary.invalid) { this.computeForm.markAllAsTouched(); return; }
    const raw = this.computeForm.getRawValue();
    this.computing.set(true);
    this.errorMessage.set(null);
    this.componentService.compute({
      basicSalary: raw.basicSalary,
      workedDays:  raw.workedDays ?? null,
      periodDays:  raw.periodDays ?? null
    }).pipe(finalize(() => this.computing.set(false))).subscribe({
      next: (res) => {
        this.computed.set(res);
        this.itemForm.patchValue({
          grossPay: String(res.grossPay),
          totalDeductions: String(res.totalDeductions)
        });
      },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not compute pay.')
    });
  }

  upsertItem(): void {
    const p = this.period();
    if (!p || this.busy()) return;
    if (this.itemForm.invalid) { this.itemForm.markAllAsTouched(); return; }
    this.busy.set(true);
    this.errorMessage.set(null);
    const raw = this.itemForm.getRawValue();
    // Carry the computed breakdown (basic + earnings + deductions) through as the
    // item's itemised lines, so the payslip detail is persisted (M24).
    const c = this.computed();
    const lines = c
      ? [
          { code: 'BASIC', name: 'Basic pay', type: 'EARNING' as const, amount: String(c.effectiveBasic) },
          ...c.lines.map((l) => ({ code: l.code, name: l.name, type: l.type, amount: String(l.amount) }))
        ]
      : undefined;
    this.payrollService.upsertItem(p.uid, {
      employeeUid:      raw.employeeUid,
      grossPay:         raw.grossPay,
      totalDeductions:  raw.totalDeductions,
      paymentMethod:    raw.paymentMethod?.trim() || null,
      paymentReference: raw.paymentReference?.trim() || null,
      note:             raw.note?.trim() || null,
      lines
    }).pipe(finalize(() => this.busy.set(false))).subscribe({
      next: () => { this.itemForm.reset({ employeeUid: '', grossPay: '0.00', totalDeductions: '0.00', paymentMethod: '', paymentReference: '', note: '' }); this.computed.set(null); this.load(p.uid); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save item.')
    });
  }

  removeItem(item: PayrollItem): void {
    const p = this.period();
    if (!p || this.busy()) return;
    if (!globalThis.confirm(`Remove ${item.employeeName || item.employeeUid} from this period?`)) return;
    this.busy.set(true);
    this.payrollService.removeItem(p.uid, item.employeeUid)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: () => this.load(p.uid),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not remove item.')
      });
  }

  verify(): void { this.transition('verify'); }
  approve(): void { this.transition('approve'); }
  pay(): void { this.transition('pay'); }

  private transition(action: 'verify' | 'approve' | 'pay'): void {
    const p = this.period();
    if (!p || this.busy()) return;
    this.busy.set(true);
    this.errorMessage.set(null);
    const req$ = action === 'verify' ? this.payrollService.verify(p.uid)
               : action === 'approve' ? this.payrollService.approve(p.uid)
               : this.payrollService.markPaid(p.uid);
    req$.pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (updated) => this.period.set(updated),
      error: (err) => this.errorMessage.set(err?.error?.message ?? `Could not ${action} period.`)
    });
  }

  cancel(): void {
    const p = this.period();
    if (!p || this.busy()) return;
    const reason = globalThis.prompt('Reason for cancelling this period? (optional)') ?? '';
    this.busy.set(true);
    this.payrollService.cancel(p.uid, reason.trim() || null)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: (updated) => this.period.set(updated),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not cancel period.')
      });
  }

  back(): void { void this.router.navigate(['/hr/payroll']); }

  employeeLabel(e: EmployeeSummary): string {
    const middle = e.middleName ? ` ${e.middleName}` : '';
    return `${e.employeeNo} — ${e.firstName}${middle} ${e.lastName}`;
  }

  statusBadgeClass(s: PayrollPeriodStatus): string {
    return 'badge ' + (this.statuses.find((x) => x.value === s)?.badgeClass ?? '');
  }
  statusLabel(s: PayrollPeriodStatus): string {
    return this.statuses.find((x) => x.value === s)?.label ?? s;
  }
}

/** Inclusive day count of a period, used to default worked-time proration. */
function periodLengthDays(p: PayrollPeriod): number | null {
  if (!p.startDate || !p.endDate) return null;
  const start = Date.parse(p.startDate);
  const end = Date.parse(p.endDate);
  if (Number.isNaN(start) || Number.isNaN(end) || end < start) return null;
  return Math.round((end - start) / 86_400_000) + 1;
}
