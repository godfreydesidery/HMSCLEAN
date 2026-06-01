import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize, forkJoin } from 'rxjs';

import { ConsultationDiagnosisService } from '../../encounter/diagnosis/consultation-diagnosis.service';
import { ConsultationDiagnosis } from '../../encounter/diagnosis/consultation-diagnosis.types';
import { PrescriptionService } from '../../encounter/prescription/prescription.service';
import {
  PRESCRIPTION_STATUSES, Prescription, PrescribingAlert, PrescriptionStatus, PrescriptionWorklistRow
} from '../../encounter/prescription/prescription.types';
import { patientClassBadgeClass, patientClassLabel } from '../../../shared/patient-class/patient-class';
import { PharmacyService } from '../../masterdata/pharmacies/pharmacy.service';
import { DispensePrescriptionComponent } from '../stock/dispense-prescription.component';
import { StockService } from '../stock/stock.service';

type Klass = 'OUTPATIENT' | 'INPATIENT' | 'OUTSIDER';

/**
 * Legacy patient-pharmacy screen ("Attend"): all of one patient's pending scripts in
 * one place, with the consultation's FINAL diagnosis and the same-medicine/unfinished-
 * course advisory, each script worked through accept → verify → approve → dispense.
 */
@Component({
  selector: 'app-patient-dispense',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './patient-dispense.component.html'
})
export class PatientDispenseComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(PrescriptionService);
  private readonly diagService = inject(ConsultationDiagnosisService);
  private readonly stockService = inject(StockService);
  private readonly pharmacyService = inject(PharmacyService);
  private readonly modal = inject(NgbModal);

  readonly classLabel = patientClassLabel;
  readonly classBadge = patientClassBadgeClass;

  private patientUid = '';
  private klass: Klass | '' = '';

  readonly scripts = signal<PrescriptionWorklistRow[]>([]);
  readonly diagnoses = signal<ConsultationDiagnosis[]>([]);
  readonly alerts = signal<PrescribingAlert[]>([]);
  readonly patientName = signal<string | null>(null);
  readonly patientNo = signal<string | null>(null);
  readonly patientClass = signal<Klass | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly busyUid = signal<string | null>(null);

  /** TRUE while the sequential "dispense all remaining" loop is running. */
  readonly bulkBusy = signal(false);

  /** Scripts in the only directly-dispensable state (APPROVED) — the same gate as the per-row Dispense button. */
  readonly dispensableScripts = computed(() => this.scripts().filter((r) => r.status === 'APPROVED'));

  ngOnInit(): void {
    this.patientUid = this.route.snapshot.paramMap.get('patientUid') ?? '';
    this.klass = (this.route.snapshot.queryParamMap.get('class') as Klass) ?? '';
    if (!this.patientUid) { this.loading.set(false); this.errorMessage.set('Missing patient.'); return; }
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.service.worklist({ patientClass: this.klass || undefined, page: 0, size: 500 })
      .pipe(finalize(() => this.loading.set(false))).subscribe({
        next: (page) => {
          const scripts = page.content.filter((r) => r.patientUid === this.patientUid);
          this.scripts.set(scripts);
          this.patientName.set(scripts[0]?.patientName ?? this.patientUid);
          this.patientNo.set(scripts[0]?.patientNo ?? null);
          this.patientClass.set(scripts[0]?.patientClass ?? null);
          this.loadDiagnosesAndAlerts(scripts);
        },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load the patient queue.')
      });
  }

  /** Enrich with the FINAL diagnosis and the drug-repeat advisory (best-effort, non-blocking). */
  private loadDiagnosesAndAlerts(scripts: PrescriptionWorklistRow[]): void {
    this.diagnoses.set([]); this.alerts.set([]);
    const cUids = [...new Set(scripts.map((s) => s.consultationUid).filter((c): c is string => !!c))];
    if (cUids.length === 0) return;
    forkJoin(cUids.map((c) => this.diagService.list(c))).subscribe({
      next: (lists) => this.diagnoses.set(lists.flat().filter((d) => d.kind === 'FINAL')),
      error: () => { /* advisory only */ }
    });
    // The worklist row has no medicineUid; pull the full scripts to resolve it, then advise.
    forkJoin(cUids.map((c) => this.service.list(c))).subscribe({
      next: (lists) => {
        const medByUid = new Map(lists.flat().map((p: Prescription) => [p.uid, p.medicineUid]));
        const medUids = [...new Set(scripts.map((s) => medByUid.get(s.uid)).filter((m): m is string => !!m))];
        if (medUids.length === 0) return;
        forkJoin(medUids.map((m) => this.service.prescribingAlerts(this.patientUid, m))).subscribe({
          next: (dtos) => this.alerts.set(dtos.flatMap((d) => d.alerts ?? [])),
          error: () => { /* advisory only */ }
        });
      },
      error: () => { /* advisory only */ }
    });
  }

  back(): void { void this.router.navigate(['/pharmacy/dispense-queue']); }

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
    if (this.busyUid() || this.bulkBusy()) return;
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
    (ref.componentInstance as DispensePrescriptionComponent).prescription = {
      uid: row.uid, prescriptionNo: row.prescriptionNo, medicineName: row.medicineName,
      dose: row.dose, frequency: row.frequency, quantity: row.quantity
    } as Prescription;
    ref.closed.subscribe((movements) => { if (movements) this.load(); });
  }

  /**
   * Convenience bulk action: dispense every directly-dispensable (APPROVED) script
   * in sequence, reusing the single-dispense service call (no new backend endpoint).
   * The per-script modal lets the pharmacist choose a pharmacy; for a clean loop we
   * resolve the pharmacy here. When exactly one active pharmacy exists (the common
   * case the modal itself auto-selects), we dispense directly from it; otherwise the
   * pharmacy is ambiguous, so we ask the pharmacist to dispense scripts individually.
   * Any per-script error is surfaced but does not stop the remaining scripts; the
   * list is refreshed once the loop finishes.
   */
  dispenseAllRemaining(): void {
    if (this.bulkBusy() || this.busyUid()) return;
    const targets = this.dispensableScripts();
    if (targets.length === 0) return;

    this.bulkBusy.set(true);
    this.errorMessage.set(null);
    this.pharmacyService.search({ active: true, size: 200, sort: 'name,asc' }).subscribe({
      next: (page) => {
        if (page.content.length !== 1) {
          this.bulkBusy.set(false);
          this.errorMessage.set(
            'More than one active pharmacy is configured, so the dispensing pharmacy is ambiguous. ' +
            'Please dispense these scripts individually so you can choose the pharmacy.'
          );
          return;
        }
        this.dispenseSequentially(page.content[0].uid, targets, 0, []);
      },
      error: (err) => {
        this.bulkBusy.set(false);
        this.errorMessage.set(err?.error?.message ?? 'Could not resolve the dispensing pharmacy.');
      }
    });
  }

  /** Dispense the target scripts one after another, collecting any failures, then refresh. */
  private dispenseSequentially(
    pharmacyUid: string, targets: PrescriptionWorklistRow[], index: number, failures: string[]
  ): void {
    if (index >= targets.length) {
      this.bulkBusy.set(false);
      if (failures.length > 0) {
        this.errorMessage.set(`Could not dispense ${failures.length} of ${targets.length} script(s): ${failures.join(', ')}.`);
      }
      this.load();
      return;
    }
    const row = targets[index];
    this.stockService.dispense(pharmacyUid, row.uid, null).subscribe({
      next: () => this.dispenseSequentially(pharmacyUid, targets, index + 1, failures),
      error: () => this.dispenseSequentially(pharmacyUid, targets, index + 1, [...failures, row.prescriptionNo])
    });
  }

  statusBadgeClass(s: PrescriptionStatus): string {
    return 'badge ' + (PRESCRIPTION_STATUSES.find((x) => x.value === s)?.badgeClass ?? '');
  }
  statusLabel(s: PrescriptionStatus): string {
    return PRESCRIPTION_STATUSES.find((x) => x.value === s)?.label ?? s;
  }
  alertClass(a: PrescribingAlert): string {
    return a.severity === 'WARN' ? 'alert-warning' : 'alert-info';
  }
}
