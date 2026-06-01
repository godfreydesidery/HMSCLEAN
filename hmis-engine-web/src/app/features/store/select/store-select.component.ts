import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { WorkingLocationService } from '../../../core/working-location/working-location.service';
import { StoreService } from '../../masterdata/stores/store.service';
import { Store } from '../../masterdata/stores/store.types';

/**
 * The "Select store" picker. The keeper picks ONE store (stashed in
 * {@link WorkingLocationService}, scoping the session); selecting one drops them straight
 * into the store workspace (stock) with the persistent action toolbar showing. The picker
 * is scoped to the keeper's affiliations via {@code mine()} (legacy
 * load_stores_by_store_person).
 */
@Component({
  selector: 'app-store-select',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './store-select.component.html'
})
export class StoreSelectComponent {
  private readonly storeService = inject(StoreService);
  private readonly workingLocation = inject(WorkingLocationService);
  private readonly router = inject(Router);

  readonly stores = signal<Store[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly current = this.workingLocation.workingStore;

  constructor() {
    this.storeService.mine()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (stores) => this.stores.set(stores),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load your stores.')
      });
  }

  /** Select a store and enter the workspace (toolbar now visible) at the stock screen. */
  select(store: Store): void {
    this.workingLocation.setStore({ uid: store.uid, name: store.name });
    void this.router.navigate(['/store/stock']);
  }
}
