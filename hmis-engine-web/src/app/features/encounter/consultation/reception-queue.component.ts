import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { PaymentType } from '../../patient/patient.types';
import { ConsultationService } from './consultation.service';
import { ConsultationSummary } from './consultation.types';

/**
 * The doctor's "from reception" queue — their BOOKED consultations whose fee is
 * settled (CASH paid) or covered (insurance). Opening a row starts the
 * consultation (BOOKED → IN_PROGRESS) and navigates to it.
 */
@Component({
  selector: 'app-reception-queue',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './reception-queue.component.html'
})
export class ReceptionQueueComponent implements OnInit {
  private readonly service = inject(ConsultationService);
  private readonly router = inject(Router);

  readonly rows = signal<ConsultationSummary[]>([]);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly openingUid = signal<string | null>(null);

  private readonly size = 20;

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.service.receptionQueue({ page: this.page(), size: this.size })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (res) => {
          this.rows.set(res.content);
          this.totalPages.set(res.totalPages);
          this.totalElements.set(res.totalElements);
        },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load the reception queue.')
      });
  }

  open(row: ConsultationSummary): void {
    if (this.openingUid()) return;
    this.openingUid.set(row.uid);
    this.errorMessage.set(null);
    this.service.start(row.uid).pipe(finalize(() => this.openingUid.set(null))).subscribe({
      next: (c) => void this.router.navigate(['/encounters', 'consultations', c.uid]),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not open the consultation.')
    });
  }

  prev(): void { if (this.page() > 0) { this.page.update((p) => p - 1); this.load(); } }
  next(): void { if (this.page() < this.totalPages() - 1) { this.page.update((p) => p + 1); this.load(); } }

  paymentBadge(pt: PaymentType): string {
    return pt === 'CASH'
      ? 'badge text-bg-light text-secondary border'
      : 'badge text-bg-success-subtle text-success-emphasis border border-success-subtle';
  }
}
