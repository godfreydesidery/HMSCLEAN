import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { PrescriptionService } from '../../encounter/prescription/prescription.service';
import {
  PRESCRIPTION_STATUSES, Prescription, PrescriptionStatus, PrescriptionWorklistRow
} from '../../encounter/prescription/prescription.types';
import {
  PATIENT_CLASS_SCOPES, PatientClassScope, patientClassBadgeClass, patientClassLabel
} from '../../../shared/patient-class/patient-class';
import { DispensePrescriptionComponent } from '../stock/dispense-prescription.component';

/**
 * The pharmacy dispensing queue — prescriptions awaiting pharmacy action,
 * scoped by patient class. Each row exposes the next lifecycle action
 * (accept → verify → approve → dispense) so the pharmacist works the script
 * from one screen, replacing the previous "search/paste a prescription" gap.
 */
@Component({
  selector: 'app-dispense-worklist',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dispense-worklist.component.html'
})
export class DispenseWorklistComponent implements OnInit {
  private readonly service = inject(PrescriptionService);
  private readonly modal = inject(NgbModal);

  readonly classes = PATIENT_CLASS_SCOPES;
  readonly classLabel = patientClassLabel;
  readonly classBadge = patientClassBadgeClass;

  readonly rows = signal<PrescriptionWorklistRow[]>([]);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly busyUid = signal<string | null>(null);
  readonly classFilter = signal<PatientClassScope | ''>('');

  private readonly size = 20;

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.service.worklist({
      patientClass: this.classFilter() || undefined,
      page: this.page(),
      size: this.size
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (res) => {
        this.rows.set(res.content);
        this.totalPages.set(res.totalPages);
        this.totalElements.set(res.totalElements);
      },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load the dispensing queue.')
    });
  }

  setClass(c: PatientClassScope | ''): void { this.classFilter.set(c); this.page.set(0); this.load(); }

  /** Label for the button that advances this row's lifecycle. */
  nextActionLabel(s: PrescriptionStatus): string {
    switch (s) {
      case 'PENDING':  return 'Accept';
      case 'ACCEPTED': return 'Verify';
      case 'HELD':     return 'Verify';
      case 'VERIFIED': return 'Approve';
      case 'APPROVED': return 'Dispense';
      default:         return '';
    }
  }

  advance(row: PrescriptionWorklistRow): void {
    if (this.busyUid()) return;
    if (row.status === 'APPROVED') { this.openDispense(row); return; }
    this.busyUid.set(row.uid);
    this.errorMessage.set(null);
    const op =
      row.status === 'PENDING'  ? this.service.accept(row.uid)
      : row.status === 'ACCEPTED' ? this.service.verify(row.uid)
      : row.status === 'HELD'     ? this.service.verify(row.uid)
      : row.status === 'VERIFIED' ? this.service.approve(row.uid)
      : null;
    if (!op) { this.busyUid.set(null); return; }
    op.pipe(finalize(() => this.busyUid.set(null))).subscribe({
      next: () => this.load(),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not advance the prescription.')
    });
  }

  private openDispense(row: PrescriptionWorklistRow): void {
    const ref = this.modal.open(DispensePrescriptionComponent, { backdrop: 'static' });
    // The dispense modal only needs uid + the display fields below.
    (ref.componentInstance as DispensePrescriptionComponent).prescription = {
      uid: row.uid,
      prescriptionNo: row.prescriptionNo,
      medicineName: row.medicineName,
      dose: row.dose,
      frequency: row.frequency,
      quantity: row.quantity
    } as Prescription;
    ref.closed.subscribe((movements) => { if (movements) this.load(); });
  }

  prev(): void { if (this.page() > 0) { this.page.update((p) => p - 1); this.load(); } }
  next(): void { if (this.page() < this.totalPages() - 1) { this.page.update((p) => p + 1); this.load(); } }

  statusBadgeClass(s: PrescriptionStatus): string {
    return 'badge ' + (PRESCRIPTION_STATUSES.find((x) => x.value === s)?.badgeClass ?? '');
  }
  statusLabel(s: PrescriptionStatus): string {
    return PRESCRIPTION_STATUSES.find((x) => x.value === s)?.label ?? s;
  }
}
