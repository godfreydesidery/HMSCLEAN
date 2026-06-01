import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { PrescriptionService } from '../../encounter/prescription/prescription.service';
import { PrescriptionWorklistRow } from '../../encounter/prescription/prescription.types';
import {
  PATIENT_CLASS_SCOPES, PatientClassScope, patientClassBadgeClass, patientClassLabel
} from '../../../shared/patient-class/patient-class';

type Klass = 'OUTPATIENT' | 'INPATIENT' | 'OUTSIDER';

interface PatientGroup {
  patientUid: string;
  patientNo: string | null;
  patientName: string | null;
  patientClass: Klass;
  pendingCount: number;
  unpaidCount: number;
  earliestRequestedAt: string;
}

/**
 * The pharmacy dispensing queue — legacy "Attend patient" model: a list of PATIENTS
 * with pending scripts (scoped by class), not a flat per-script list. Picking a
 * patient opens their per-patient dispensing screen with all their scripts together,
 * the final diagnosis, and the drug-repeat advisory.
 */
@Component({
  selector: 'app-dispense-worklist',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dispense-worklist.component.html'
})
export class DispenseWorklistComponent implements OnInit {
  private readonly service = inject(PrescriptionService);
  private readonly router = inject(Router);

  readonly classes = PATIENT_CLASS_SCOPES;
  readonly classLabel = patientClassLabel;
  readonly classBadge = patientClassBadgeClass;

  readonly rows = signal<PrescriptionWorklistRow[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly classFilter = signal<PatientClassScope | ''>('');
  /** Pay-before-service gate: off (default) hides unpaid ambulatory scripts. */
  readonly showUnpaid = signal(false);

  // The queue is bounded (only pending scripts) so we pull it in one page and group
  // by patient client-side; counts are correct because there is no server paging.
  private readonly size = 500;

  readonly patients = computed<PatientGroup[]>(() => {
    const byPatient = new Map<string, PatientGroup>();
    for (const r of this.rows()) {
      const g = byPatient.get(r.patientUid);
      if (g) {
        g.pendingCount++;
        if (!r.settled) g.unpaidCount++;
        if (r.requestedAt < g.earliestRequestedAt) g.earliestRequestedAt = r.requestedAt;
      } else {
        byPatient.set(r.patientUid, {
          patientUid: r.patientUid, patientNo: r.patientNo, patientName: r.patientName,
          patientClass: r.patientClass, pendingCount: 1, unpaidCount: r.settled ? 0 : 1,
          earliestRequestedAt: r.requestedAt
        });
      }
    }
    return [...byPatient.values()].sort((a, b) => (a.earliestRequestedAt < b.earliestRequestedAt ? -1 : 1));
  });

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.service.worklist({
      patientClass: this.classFilter() || undefined,
      hideUnpaid: !this.showUnpaid(),
      page: 0, size: this.size
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
        next: (res) => this.rows.set(res.content),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load the dispensing queue.')
      });
  }

  setClass(c: PatientClassScope | ''): void { this.classFilter.set(c); this.load(); }
  toggleUnpaid(): void { this.showUnpaid.update((v) => !v); this.load(); }

  attend(g: PatientGroup): void {
    void this.router.navigate(['/pharmacy/dispense-queue/patient', g.patientUid], {
      queryParams: { class: g.patientClass }
    });
  }
}
