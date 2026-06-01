import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { AcceptTransferModalComponent } from './accept-transfer-modal.component';
import { ConsultationService } from './consultation.service';
import { Consultation, ConsultationTransfer } from './consultation.types';

/**
 * Reception's incoming-transfer queue (reworked Phase 44): every PENDING transfer
 * raised by a treating doctor. Accept books the receiving consultation (choosing
 * the clinician) and navigates to it; Cancel reverts the transfer and returns the
 * source consultation to IN_PROGRESS.
 */
@Component({
  selector: 'app-transfer-queue',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './transfer-queue.component.html'
})
export class TransferQueueComponent implements OnInit {
  private readonly service = inject(ConsultationService);
  private readonly router = inject(Router);
  private readonly modal = inject(NgbModal);

  readonly rows = signal<ConsultationTransfer[]>([]);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly busyUid = signal<string | null>(null);

  private readonly size = 20;

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.service.transferQueue({ status: 'PENDING', page: this.page(), size: this.size })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (res) => {
          this.rows.set(res.content);
          this.totalPages.set(res.totalPages);
          this.totalElements.set(res.totalElements);
        },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load the transfer queue.')
      });
  }

  accept(row: ConsultationTransfer): void {
    if (this.busyUid()) return;
    const ref = this.modal.open(AcceptTransferModalComponent, { backdrop: 'static' });
    (ref.componentInstance as AcceptTransferModalComponent).transfer = row;
    ref.closed.subscribe((consultation: Consultation | undefined) => {
      if (consultation?.uid) void this.router.navigate(['/encounters', 'consultations', consultation.uid]);
    });
  }

  cancel(row: ConsultationTransfer): void {
    if (this.busyUid()) return;
    const reason = globalThis.prompt('Reason for cancelling this transfer?')?.trim() || null;
    this.busyUid.set(row.uid);
    this.errorMessage.set(null);
    this.service.cancelTransfer(row.uid, { reason })
      .pipe(finalize(() => this.busyUid.set(null)))
      .subscribe({
        next: () => this.load(),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not cancel the transfer.')
      });
  }

  prev(): void { if (this.page() > 0) { this.page.update((p) => p - 1); this.load(); } }
  next(): void { if (this.page() < this.totalPages() - 1) { this.page.update((p) => p + 1); this.load(); } }
}
