import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { ADMISSION_STATUSES, AdmissionStatus } from '../encounter/admission/admission.types';
import { WardService } from '../masterdata/wards/ward.service';
import { Ward } from '../masterdata/wards/ward.types';
import { ReportingService } from './reporting.service';
import { IpdRegisterEntry } from './reporting.types';

function isoToday(): string {
  return new Date().toISOString().slice(0, 10);
}

function isoMonthStart(): string {
  const d = new Date();
  d.setDate(1);
  return d.toISOString().slice(0, 10);
}

@Component({
  selector: 'app-ipd-register-report',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './ipd-register-report.component.html'
})
export class IpdRegisterReportComponent {
  private readonly reportingService = inject(ReportingService);
  private readonly wardService = inject(WardService);
  private readonly fb = inject(FormBuilder);

  readonly statuses = ADMISSION_STATUSES;
  readonly wards = signal<Ward[]>([]);

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly entries = signal<IpdRegisterEntry[]>([]);
  readonly searched = signal(false);

  readonly form = this.fb.nonNullable.group({
    from: [isoMonthStart(), [Validators.required]],
    to: [isoToday(), [Validators.required]],
    wardUid: [''],
    status: ['' as AdmissionStatus | '']
  });

  constructor() {
    this.wardService.search({ active: true, size: 200, sort: 'name,asc' }).subscribe({
      next: (page) => this.wards.set(page.content),
      error: () => { /* ward filter is optional — fall back to free-text uid */ }
    });
    this.run();
  }

  run(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { from, to, wardUid, status } = this.form.getRawValue();
    this.loading.set(true);
    this.errorMessage.set(null);
    this.reportingService.ipdRegister({
      from,
      to,
      wardUid: wardUid || undefined,
      status: (status || undefined) as AdmissionStatus | undefined
    })
      .pipe(finalize(() => { this.loading.set(false); this.searched.set(true); }))
      .subscribe({
        next: (rows) => this.entries.set(rows),
        error: (err) => { this.entries.set([]); this.errorMessage.set(err?.error?.message ?? 'Could not load IPD register.'); }
      });
  }

  statusBadgeClass(s: string): string {
    return 'badge ' + (this.statuses.find((x) => x.value === s)?.badgeClass ?? 'text-bg-light border');
  }

  statusLabel(s: string): string {
    return this.statuses.find((x) => x.value === s)?.label ?? s;
  }
}
