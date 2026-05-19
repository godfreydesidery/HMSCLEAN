import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AssetService } from './asset.service';
import { ASSET_STATUSES, Asset, AssetStatus, RETIRE_TARGETS } from './asset.types';

@Component({
  selector: 'app-asset-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './asset-detail.component.html'
})
export class AssetDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly assetService = inject(AssetService);

  readonly statuses = ASSET_STATUSES;
  readonly retireTargets = RETIRE_TARGETS;

  readonly asset = signal<Asset | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly canEdit       = computed(() => this.asset()?.status === 'ACTIVE' || this.asset()?.status === 'RETIRED');
  readonly canRetire     = computed(() => {
    const s = this.asset()?.status;
    return s === 'ACTIVE' || s === 'RETIRED' || s === 'LOST';
  });
  readonly canReinstate  = computed(() => this.asset()?.status === 'RETIRED');

  constructor() {
    const uid = this.route.snapshot.paramMap.get('uid');
    if (!uid) {
      this.loading.set(false);
      this.errorMessage.set('Missing asset identifier.');
      return;
    }
    this.load(uid);
  }

  private load(uid: string): void {
    this.loading.set(true);
    this.assetService.findByUid(uid)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (a) => this.asset.set(a),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load asset.')
      });
  }

  retire(target: AssetStatus): void {
    const current = this.asset();
    if (!current || this.busy()) return;
    const reason = globalThis.prompt(`Reason for marking the asset ${target}? (optional)`) ?? '';
    const date = new Date().toISOString().slice(0, 10);
    this.busy.set(true);
    this.errorMessage.set(null);
    this.assetService.retire(current.uid, { target, date, reason: reason.trim() || null })
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: (a) => this.asset.set(a),
        error: (err) => this.errorMessage.set(err?.error?.message ?? `Could not move asset to ${target}.`)
      });
  }

  reinstate(): void {
    const current = this.asset();
    if (!current || this.busy()) return;
    this.busy.set(true);
    this.errorMessage.set(null);
    this.assetService.reinstate(current.uid)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: (a) => this.asset.set(a),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not reinstate asset.')
      });
  }

  edit(): void {
    const current = this.asset();
    if (current) void this.router.navigate(['/hr/assets', current.uid, 'edit']);
  }

  statusBadgeClass(s: AssetStatus): string {
    return 'badge ' + (this.statuses.find((x) => x.value === s)?.badgeClass ?? '');
  }
  statusLabel(s: AssetStatus): string {
    return this.statuses.find((x) => x.value === s)?.label ?? s;
  }
}
