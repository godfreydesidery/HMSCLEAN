import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NgbDropdownModule } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { ReturnService } from './return.service';
import { ReturnDto, ReturnStatus, returnBadgeClass, returnLabel } from './return.types';

@Component({
  selector: 'app-return-detail',
  standalone: true,
  imports: [CommonModule, NgbDropdownModule],
  templateUrl: './return-detail.component.html'
})
export class ReturnDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly returnService = inject(ReturnService);

  readonly ret = signal<ReturnDto | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly actionMessage = signal<string | null>(null);

  /** DRAFT → submit (sends to store) or cancel (pharmacy walks it back). */
  readonly canSubmit = computed(() => this.ret()?.status === 'DRAFT');
  readonly canCancel = computed(() => this.ret()?.status === 'DRAFT');
  /** SUBMITTED → complete (store accepts) or reject (store won't take it back). */
  readonly canComplete = computed(() => this.ret()?.status === 'SUBMITTED');
  readonly canReject = computed(() => this.ret()?.status === 'SUBMITTED');

  constructor() {
    const uid = this.route.snapshot.paramMap.get('uid');
    if (!uid) {
      this.errorMessage.set('Missing return identifier.');
      this.loading.set(false);
      return;
    }
    this.load(uid);
  }

  private load(uid: string): void {
    this.loading.set(true);
    this.returnService.findByUid(uid).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (ret) => this.ret.set(ret),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load return.')
    });
  }

  back(): void { void this.router.navigate(['..'], { relativeTo: this.route }); }

  submit(): void { this.runAction((uid) => this.returnService.submit(uid), 'Return submitted.'); }
  complete(): void { this.runAction((uid) => this.returnService.complete(uid), 'Return completed.'); }
  cancel(): void { this.runAction((uid) => this.returnService.cancel(uid), 'Return cancelled.'); }

  reject(): void {
    const reason = globalThis.prompt('Reason for rejecting this return?')?.trim() ?? null;
    if (reason === null) return;
    this.runAction((uid) => this.returnService.reject(uid, reason || null), 'Return rejected.');
  }

  private runAction(call: (uid: string) => ReturnType<ReturnService['submit']>, successMsg: string): void {
    const o = this.ret(); if (!o || this.busy()) return;
    this.busy.set(true);
    this.errorMessage.set(null);
    this.actionMessage.set(null);
    call(o.uid).pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (ret) => { this.ret.set(ret); this.actionMessage.set(successMsg); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Action could not be completed.')
    });
  }

  statusBadgeClass(s: ReturnStatus): string { return returnBadgeClass(s); }
  statusLabel(s: ReturnStatus): string { return returnLabel(s); }
}
