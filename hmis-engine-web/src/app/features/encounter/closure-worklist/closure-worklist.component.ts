import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { ClosureWorklistService } from './closure-worklist.service';
import { CLOSURE_SUBJECT_FILTERS, ClosureSubject, ClosureWorklistItem } from './closure-worklist.types';
import { DischargePlanKind } from '../admission/discharge-plan.types';

/**
 * The closure worklist (DISCH-1): every PENDING closure plan — inpatient
 * discharge/death/referral AND outpatient death/referral — awaiting a second
 * approver, in one queue. Opening a row deep-links to the subject's detail page
 * (admission or consultation) where the approve / cancel actions live.
 */
@Component({
  selector: 'app-closure-worklist',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './closure-worklist.component.html'
})
export class ClosureWorklistComponent implements OnInit {
  private readonly service = inject(ClosureWorklistService);
  private readonly router = inject(Router);

  readonly filters = CLOSURE_SUBJECT_FILTERS;
  readonly subjectType = signal<ClosureSubject | ''>('');

  readonly rows = signal<ClosureWorklistItem[]>([]);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  private readonly size = 20;

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.service.worklist({ subjectType: this.subjectType(), page: this.page(), size: this.size })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (res) => {
          this.rows.set(res.content);
          this.totalPages.set(res.totalPages);
          this.totalElements.set(res.totalElements);
        },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load the closure worklist.')
      });
  }

  changeFilter(value: ClosureSubject | ''): void {
    this.subjectType.set(value);
    this.page.set(0);
    this.load();
  }

  open(row: ClosureWorklistItem): void {
    if (row.subjectType === 'ADMISSION' && row.admissionUid) {
      void this.router.navigate(['/encounters', 'admissions', row.admissionUid]);
    } else if (row.subjectType === 'CONSULTATION' && row.consultationUid) {
      void this.router.navigate(['/encounters', 'consultations', row.consultationUid]);
    }
  }

  kindBadge(kind: DischargePlanKind): string {
    switch (kind) {
      case 'DISCHARGE': return 'text-bg-success';
      case 'DECEASED':  return 'text-bg-dark';
      case 'REFERRAL':  return 'text-bg-info';
      default:          return 'text-bg-secondary';
    }
  }

  kindLabel(kind: DischargePlanKind): string {
    switch (kind) {
      case 'DISCHARGE': return 'Discharge';
      case 'DECEASED':  return 'Deceased';
      case 'REFERRAL':  return 'Referral';
      default:          return kind;
    }
  }

  prev(): void { if (this.page() > 0) { this.page.update((p) => p - 1); this.load(); } }
  next(): void { if (this.page() < this.totalPages() - 1) { this.page.update((p) => p + 1); this.load(); } }
}
