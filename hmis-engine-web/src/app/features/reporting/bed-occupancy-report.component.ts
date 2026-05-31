import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { ReportingService } from './reporting.service';
import { BedOccupancyEntry } from './reporting.types';

@Component({
  selector: 'app-bed-occupancy-report',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './bed-occupancy-report.component.html'
})
export class BedOccupancyReportComponent {
  private readonly reportingService = inject(ReportingService);

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly entries = signal<BedOccupancyEntry[]>([]);

  readonly totals = computed(() => {
    return this.entries().reduce(
      (acc, e) => {
        acc.capacity += e.capacity;
        acc.beds += e.beds;
        acc.occupied += e.occupied;
        acc.free += e.free;
        acc.outOfService += e.outOfService;
        return acc;
      },
      { capacity: 0, beds: 0, occupied: 0, free: 0, outOfService: 0 }
    );
  });

  constructor() {
    this.run();
  }

  run(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.reportingService.bedOccupancy()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (rows) => this.entries.set(rows),
        error: (err) => { this.entries.set([]); this.errorMessage.set(err?.error?.message ?? 'Could not load bed occupancy.'); }
      });
  }

  occupancyPct(e: BedOccupancyEntry): number {
    return e.beds > 0 ? Math.round((e.occupied / e.beds) * 100) : 0;
  }
}
