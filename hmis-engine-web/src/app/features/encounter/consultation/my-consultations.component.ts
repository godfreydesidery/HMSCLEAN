import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { ConsultationService } from './consultation.service';
import { CONSULTATION_STATUSES, ConsultationStatus, ConsultationSummary } from './consultation.types';

/**
 * The signed-in clinician's own active consultations (BOOKED / IN_PROGRESS /
 * TRANSFERRED), newest first. Each row links to the consultation detail.
 */
@Component({
  selector: 'app-my-consultations',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './my-consultations.component.html'
})
export class MyConsultationsComponent implements OnInit {
  private readonly service = inject(ConsultationService);
  private readonly router = inject(Router);

  readonly statuses = CONSULTATION_STATUSES;
  readonly rows = signal<ConsultationSummary[]>([]);
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
    this.service.myOpen({ page: this.page(), size: this.size })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (res) => {
          this.rows.set(res.content);
          this.totalPages.set(res.totalPages);
          this.totalElements.set(res.totalElements);
        },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load your consultations.')
      });
  }

  prev(): void { if (this.page() > 0) { this.page.update((p) => p - 1); this.load(); } }
  next(): void { if (this.page() < this.totalPages() - 1) { this.page.update((p) => p + 1); this.load(); } }

  view(c: ConsultationSummary): void { void this.router.navigate(['/encounters', 'consultations', c.uid]); }

  statusBadgeClass(s: ConsultationStatus): string {
    return 'badge ' + (this.statuses.find((x) => x.value === s)?.badgeClass ?? '');
  }

  statusLabel(s: ConsultationStatus): string {
    return this.statuses.find((x) => x.value === s)?.label ?? s;
  }
}
