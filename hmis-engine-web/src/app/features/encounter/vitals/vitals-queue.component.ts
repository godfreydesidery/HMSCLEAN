import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { VitalsFormComponent } from './vitals-form.component';
import { VitalsService } from './vitals.service';
import { VitalsStatus, VitalsWorklistRow } from './vitals.types';

/**
 * The OUTPATIENT nurse-triage worklist (OPC-3): fee-settled BOOKED / IN_PROGRESS
 * consultations that may still need vitals. Opening a row launches the nurse
 * vitals form (fill → save → submit). Distinct from the inpatient (admission-
 * scoped) nursing worklist at /encounters/nurse-queue.
 */
@Component({
  selector: 'app-vitals-queue',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './vitals-queue.component.html'
})
export class VitalsQueueComponent implements OnInit {
  private readonly service = inject(VitalsService);
  private readonly modal = inject(NgbModal);

  readonly rows = signal<VitalsWorklistRow[]>([]);
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
    this.service.nurseWorklist({ page: this.page(), size: this.size })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (res) => {
          this.rows.set(res.content);
          this.totalPages.set(res.totalPages);
          this.totalElements.set(res.totalElements);
        },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load the vitals worklist.')
      });
  }

  open(row: VitalsWorklistRow): void {
    const ref = this.modal.open(VitalsFormComponent, { size: 'lg', backdrop: 'static' });
    (ref.componentInstance as VitalsFormComponent).consultationUid = row.consultationUid;
    // Refresh the worklist on save/submit so the status badge reflects the new state.
    ref.closed.subscribe(() => this.load());
  }

  // ----- vitals-status presentation -------------------------------------

  vitalsBadgeClass(s: VitalsStatus | null): string {
    switch (s) {
      case 'PENDING': return 'badge text-bg-warning';
      case 'SUBMITTED': return 'badge text-bg-info';
      case 'ARCHIVED': return 'badge text-bg-success';
      default: return 'badge text-bg-secondary'; // EMPTY / null → needs vitals
    }
  }

  vitalsLabel(s: VitalsStatus | null): string {
    switch (s) {
      case 'PENDING': return 'In progress';
      case 'SUBMITTED': return 'Ready for doctor';
      case 'ARCHIVED': return 'Done';
      default: return 'Needs vitals'; // EMPTY / null
    }
  }

  prev(): void { if (this.page() > 0) { this.page.update((p) => p - 1); this.load(); } }
  next(): void { if (this.page() < this.totalPages() - 1) { this.page.update((p) => p + 1); this.load(); } }
}
