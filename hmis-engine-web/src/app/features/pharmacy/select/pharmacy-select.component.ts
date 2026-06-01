import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { WorkingLocationService } from '../../../core/working-location/working-location.service';
import { PharmacyService } from '../../masterdata/pharmacies/pharmacy.service';
import { Pharmacy } from '../../masterdata/pharmacies/pharmacy.types';

/**
 * The "Select pharmacy" picker. The pharmacist picks ONE pharmacy (stashed in
 * {@link WorkingLocationService}, scoping the session); selecting one drops them straight
 * into the pharmacy workspace (dispensing queue) with the persistent action toolbar
 * showing. Legacy lets any pharmacist pick any active pharmacy, so the picker is NOT
 * affiliation-scoped.
 */
@Component({
  selector: 'app-pharmacy-select',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pharmacy-select.component.html'
})
export class PharmacySelectComponent {
  private readonly pharmacyService = inject(PharmacyService);
  private readonly workingLocation = inject(WorkingLocationService);
  private readonly router = inject(Router);

  readonly pharmacies = signal<Pharmacy[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly query = signal('');

  readonly current = this.workingLocation.workingPharmacy;

  readonly filtered = computed(() => {
    const q = this.query().trim().toLowerCase();
    const all = this.pharmacies();
    if (!q) return all;
    return all.filter((p) => p.name.toLowerCase().includes(q) || p.code.toLowerCase().includes(q));
  });

  constructor() {
    this.pharmacyService.search({ active: true, size: 200, sort: 'name,asc' })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.pharmacies.set(page.content),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load pharmacies.')
      });
  }

  /** Select a pharmacy and enter the workspace (toolbar now visible) at the dispensing queue. */
  select(pharmacy: Pharmacy): void {
    this.workingLocation.setPharmacy({ uid: pharmacy.uid, name: pharmacy.name });
    void this.router.navigate(['/pharmacy/dispense-queue']);
  }
}
