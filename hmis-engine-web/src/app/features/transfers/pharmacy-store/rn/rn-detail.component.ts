import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { ReceiveNoteStatus, receiveNoteBadgeClass, receiveNoteLabel } from '../../transfer-common.types';
import { RnService } from './rn.service';
import { RNDto } from './rn.types';

@Component({
  selector: 'app-rn-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './rn-detail.component.html'
})
export class RnDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly rnService = inject(RnService);

  readonly note = signal<RNDto | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  constructor() {
    const uid = this.route.snapshot.paramMap.get('uid');
    if (!uid) {
      this.errorMessage.set('Missing receive note identifier.');
      this.loading.set(false);
      return;
    }
    this.load(uid);
  }

  private load(uid: string): void {
    this.loading.set(true);
    this.rnService.findByUid(uid)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (rn) => this.note.set(rn),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load the receive note.')
      });
  }

  back(): void { void this.router.navigate(['..'], { relativeTo: this.route }); }

  statusBadgeClass(s: ReceiveNoteStatus): string { return receiveNoteBadgeClass(s); }
  statusLabel(s: ReceiveNoteStatus): string { return receiveNoteLabel(s); }
}
