import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgbDropdownModule } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { TransferDocStatus, transferDocBadgeClass, transferDocLabel } from '../../transfer-common.types';
import { ToService } from './to.service';
import { TODto } from './to.types';

@Component({
  selector: 'app-to-detail',
  standalone: true,
  imports: [CommonModule, NgbDropdownModule, RouterLink],
  templateUrl: './to-detail.component.html'
})
export class ToDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toService = inject(ToService);

  readonly order = signal<TODto | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly actionMessage = signal<string | null>(null);

  readonly canVerify = computed(() => this.order()?.status === 'PENDING');
  readonly canApprove = computed(() => this.order()?.status === 'VERIFIED');
  readonly canIssue = computed(() => this.order()?.status === 'APPROVED');
  readonly canReject = computed(() => {
    const s = this.order()?.status;
    return s === 'PENDING' || s === 'VERIFIED' || s === 'APPROVED';
  });
  readonly hasActions = computed(() => this.canVerify() || this.canApprove() || this.canIssue() || this.canReject());

  constructor() {
    const uid = this.route.snapshot.paramMap.get('uid');
    if (!uid) {
      this.errorMessage.set('Missing transfer order identifier.');
      this.loading.set(false);
      return;
    }
    this.load(uid);
  }

  private load(uid: string): void {
    this.loading.set(true);
    this.toService.findByUid(uid).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (to) => this.order.set(to),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load transfer order.')
    });
  }

  back(): void { void this.router.navigate(['/transfers/to']); }

  verify(): void {
    const o = this.order(); if (!o) return;
    this.runAction(this.toService.verify(o.uid), 'Transfer order verified.');
  }
  approve(): void {
    const o = this.order(); if (!o) return;
    this.runAction(this.toService.approve(o.uid), 'Transfer order approved.');
  }
  issue(): void {
    const o = this.order(); if (!o) return;
    if (!globalThis.confirm('Issue stock for this transfer order? Batches will be picked and store stock reduced.')) return;
    this.runAction(this.toService.issue(o.uid), 'Stock issued.');
  }
  reject(): void {
    const o = this.order(); if (!o) return;
    const reason = globalThis.prompt('Reason for rejecting this transfer order?')?.trim() ?? null;
    if (reason === null) return;
    this.runAction(this.toService.reject(o.uid, reason || null), 'Transfer order rejected.');
  }

  private runAction(obs: ReturnType<ToService['verify']>, success: string): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.errorMessage.set(null);
    this.actionMessage.set(null);
    obs.pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (to) => { this.order.set(to); this.actionMessage.set(success); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Action failed.')
    });
  }

  statusBadgeClass(s: TransferDocStatus): string { return transferDocBadgeClass(s); }
  statusLabel(s: TransferDocStatus): string { return transferDocLabel(s); }
}
