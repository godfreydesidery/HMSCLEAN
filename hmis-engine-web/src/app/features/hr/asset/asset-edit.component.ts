import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { finalize } from 'rxjs';

import { AssetFormComponent } from './asset-form.component';
import { AssetService } from './asset.service';
import { Asset } from './asset.types';

@Component({
  selector: 'app-asset-edit',
  standalone: true,
  imports: [CommonModule, AssetFormComponent],
  template: `
    @if (loading()) {
      <div class="hmis-empty"><i class="bi bi-hourglass-split"></i><div class="small">Loading asset…</div></div>
    } @else if (errorMessage()) {
      <div class="alert alert-danger">{{ errorMessage() }}</div>
    } @else {
      @if (asset(); as a) {
        <app-asset-form [existing]="a"></app-asset-form>
      }
    }
  `
})
export class AssetEditComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly assetService = inject(AssetService);

  readonly asset = signal<Asset | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  constructor() {
    const uid = this.route.snapshot.paramMap.get('uid');
    if (!uid) {
      this.loading.set(false);
      this.errorMessage.set('Missing asset identifier.');
      return;
    }
    this.assetService.findByUid(uid)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (a) => this.asset.set(a),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load asset.')
      });
  }
}
