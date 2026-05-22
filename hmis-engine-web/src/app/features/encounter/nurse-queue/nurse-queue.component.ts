import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { AdmissionService } from '../admission/admission.service';
import { AdmissionSummary } from '../admission/admission.types';

/**
 * The nurse worklist — currently-admitted patients whose nursing chart, vitals
 * and consumables are open for entry. Opening a row goes to the admission detail
 * where the existing chart / vitals / consumable views live.
 */
@Component({
  selector: 'app-nurse-queue',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './nurse-queue.component.html'
})
export class NurseQueueComponent implements OnInit {
  private readonly service = inject(AdmissionService);
  private readonly router = inject(Router);

  readonly rows = signal<AdmissionSummary[]>([]);
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
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load the nurse worklist.')
      });
  }

  open(row: AdmissionSummary): void {
    void this.router.navigate(['/encounters', 'admissions', row.uid]);
  }

  prev(): void { if (this.page() > 0) { this.page.update((p) => p - 1); this.load(); } }
  next(): void { if (this.page() < this.totalPages() - 1) { this.page.update((p) => p + 1); this.load(); } }
}
