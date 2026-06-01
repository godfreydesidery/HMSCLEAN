import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NgbDropdownModule } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { TransferDocStatus, transferDocBadgeClass, transferDocLabel } from '../../transfer-common.types';
import { PpROService } from './pp-ro.service';
import { RODto } from './pp-ro.types';

@Component({
  selector: 'app-pp-ro-detail',
  standalone: true,
  imports: [CommonModule, NgbDropdownModule],
  templateUrl: './pp-ro-detail.component.html'
})
export class PpRoDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly roService = inject(PpROService);

  readonly ro = signal<RODto | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly actionMessage = signal<string | null>(null);

  readonly canVerify = computed(() => this.ro()?.status === 'PENDING');
  readonly canApprove = computed(() => this.ro()?.status === 'VERIFIED');
  readonly canSubmit = computed(() => this.ro()?.status === 'APPROVED');
  /** Reject / return are open while the document is still being processed. */
  readonly canRejectOrReturn = computed(() => {
    const s = this.ro()?.status;
    return s === 'PENDING' || s === 'VERIFIED' || s === 'APPROVED';
  });

  constructor() {
    const uid = this.route.snapshot.paramMap.get('uid');
    if (!uid) {
      this.errorMessage.set('Missing requisition identifier.');
      this.loading.set(false);
      return;
    }
    this.load(uid);
  }

  private load(uid: string): void {
    this.loading.set(true);
    this.roService.findByUid(uid).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (ro) => this.ro.set(ro),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load requisition.')
    });
  }

  back(): void { void this.router.navigate(['..'], { relativeTo: this.route }); }

  verify(): void { this.runAction((uid) => this.roService.verify(uid), 'Requisition verified.'); }
  approve(): void { this.runAction((uid) => this.roService.approve(uid), 'Requisition approved.'); }
  submit(): void { this.runAction((uid) => this.roService.submit(uid), 'Requisition submitted.'); }

  reject(): void {
    const reason = globalThis.prompt('Reason for rejecting this requisition?')?.trim() ?? null;
    if (reason === null) return;
    this.runAction((uid) => this.roService.reject(uid, reason || null), 'Requisition rejected.');
  }

  return(): void {
    const reason = globalThis.prompt('Reason for returning this requisition?')?.trim() ?? null;
    if (reason === null) return;
    this.runAction((uid) => this.roService.return(uid, reason || null), 'Requisition returned.');
  }

  private runAction(call: (uid: string) => ReturnType<PpROService['verify']>, successMsg: string): void {
    const o = this.ro(); if (!o || this.busy()) return;
    this.busy.set(true);
    this.errorMessage.set(null);
    this.actionMessage.set(null);
    call(o.uid).pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (ro) => { this.ro.set(ro); this.actionMessage.set(successMsg); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Action could not be completed.')
    });
  }

  statusBadgeClass(s: TransferDocStatus): string { return transferDocBadgeClass(s); }
  statusLabel(s: TransferDocStatus): string { return transferDocLabel(s); }
}
