import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { ClaimService } from './claim.service';
import { CLAIM_STATUSES, Claim, ClaimStatus } from './claim.types';
import { RecordSettlementComponent } from './record-settlement.component';
import { RejectClaimComponent } from './reject-claim.component';

@Component({
  selector: 'app-claim-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './claim-detail.component.html',
  styleUrl: './claim-detail.component.scss'
})
export class ClaimDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly claimService = inject(ClaimService);
  private readonly modal = inject(NgbModal);

  readonly statuses = CLAIM_STATUSES;
  readonly claim = signal<Claim | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly canSubmit = computed(() => this.claim()?.status === 'DRAFT');
  readonly canSettle = computed(() => {
    const s = this.claim()?.status;
    return s === 'SUBMITTED' || s === 'PARTIALLY_SETTLED';
  });
  readonly canReject = computed(() => {
    const s = this.claim()?.status;
    return s === 'SUBMITTED' || s === 'PARTIALLY_SETTLED';
  });
  readonly canDiscard = computed(() => this.claim()?.status === 'DRAFT');

  constructor() {
    const uid = this.route.snapshot.paramMap.get('uid');
    if (!uid) { this.loading.set(false); this.errorMessage.set('Missing claim identifier.'); return; }
    this.claimService.findByUid(uid).subscribe({
      next: (c) => { this.claim.set(c); this.loading.set(false); },
      error: (err) => { this.errorMessage.set(err?.error?.message ?? 'Could not load claim.'); this.loading.set(false); }
    });
  }

  back(): void { void this.router.navigate(['/billing/claims']); }

  submit(): void {
    const c = this.claim(); if (!c) return;
    this.claimService.submit(c.uid).subscribe({
      next: (updated) => this.claim.set(updated),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not submit claim.')
    });
  }

  recordSettlement(): void {
    const c = this.claim(); if (!c) return;
    const ref = this.modal.open(RecordSettlementComponent, { backdrop: 'static' });
    (ref.componentInstance as RecordSettlementComponent).claim = c;
    ref.closed.subscribe((updated: Claim | undefined) => {
      if (updated) this.claim.set(updated);
    });
  }

  reject(): void {
    const c = this.claim(); if (!c) return;
    const ref = this.modal.open(RejectClaimComponent, { backdrop: 'static' });
    (ref.componentInstance as RejectClaimComponent).claim = c;
    ref.closed.subscribe((updated: Claim | undefined) => {
      if (updated) this.claim.set(updated);
    });
  }

  discard(): void {
    const c = this.claim(); if (!c) return;
    if (!globalThis.confirm('Discard this draft claim? This cannot be undone.')) return;
    this.claimService.discard(c.uid).subscribe({
      next: () => this.back(),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not discard claim.')
    });
  }

  statusBadgeClass(s: ClaimStatus): string {
    return 'badge ' + (this.statuses.find((x) => x.value === s)?.badgeClass ?? '');
  }
  statusLabel(s: ClaimStatus): string {
    return this.statuses.find((x) => x.value === s)?.label ?? s;
  }
}
