import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { forkJoin } from 'rxjs';

import { VitalsFormComponent } from '../vitals/vitals-form.component';
import { VitalsService } from '../vitals/vitals.service';
import { PatientVitals } from '../vitals/vitals.types';
import { ConsultationService } from './consultation.service';
import { CONSULTATION_STATUSES, Consultation, ConsultationStatus } from './consultation.types';

type TabKey = 'overview' | 'vitals' | 'diagnoses' | 'orders';

@Component({
  selector: 'app-consultation-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './consultation-detail.component.html',
  styleUrl: './consultation-detail.component.scss'
})
export class ConsultationDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly consultationService = inject(ConsultationService);
  private readonly vitalsService = inject(VitalsService);
  private readonly modal = inject(NgbModal);

  readonly statuses = CONSULTATION_STATUSES;
  readonly consultation = signal<Consultation | null>(null);
  readonly vitals = signal<PatientVitals[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly activeTab = signal<TabKey>('overview');

  readonly canStart = computed(() => this.consultation()?.status === 'BOOKED');
  readonly canComplete = computed(() => this.consultation()?.status === 'IN_PROGRESS');
  readonly canCancel = computed(() => {
    const s = this.consultation()?.status;
    return s === 'BOOKED' || s === 'IN_PROGRESS';
  });
  readonly canRecordVitals = computed(() => {
    const s = this.consultation()?.status;
    return s === 'BOOKED' || s === 'IN_PROGRESS';
  });

  constructor() {
    const uid = this.route.snapshot.paramMap.get('uid');
    if (!uid) {
      this.loading.set(false);
      this.errorMessage.set('Missing consultation identifier.');
      return;
    }
    forkJoin({
      consultation: this.consultationService.findByUid(uid),
      vitals: this.vitalsService.list(uid)
    }).subscribe({
      next: ({ consultation, vitals }) => {
        this.consultation.set(consultation);
        this.vitals.set(vitals);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.message ?? 'Could not load consultation.');
        this.loading.set(false);
      }
    });
  }

  setTab(tab: TabKey): void { this.activeTab.set(tab); }
  back(): void { void this.router.navigate(['/encounters', 'consultations']); }

  start(): void {
    const c = this.consultation();
    if (!c) return;
    this.consultationService.start(c.uid).subscribe({
      next: (updated) => this.consultation.set(updated),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not start consultation.')
    });
  }

  complete(): void {
    const c = this.consultation();
    if (!c) return;
    if (!globalThis.confirm('Mark this consultation as completed?')) return;
    this.consultationService.complete(c.uid).subscribe({
      next: (updated) => this.consultation.set(updated),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not complete consultation.')
    });
  }

  cancel(): void {
    const c = this.consultation();
    if (!c) return;
    const reason = globalThis.prompt('Reason for cancelling this consultation?')?.trim() ?? null;
    this.consultationService.cancel(c.uid, reason).subscribe({
      next: (updated) => this.consultation.set(updated),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not cancel consultation.')
    });
  }

  recordVitals(): void {
    const c = this.consultation();
    if (!c) return;
    const ref = this.modal.open(VitalsFormComponent, { size: 'lg', backdrop: 'static' });
    (ref.componentInstance as VitalsFormComponent).consultationUid = c.uid;
    ref.closed.subscribe(() => this.refreshVitals());
  }

  deleteVitals(v: PatientVitals): void {
    if (!globalThis.confirm('Delete this vitals reading?')) return;
    const c = this.consultation();
    if (!c) return;
    this.vitalsService.delete(c.uid, v.uid).subscribe({
      next: () => this.refreshVitals(),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not delete vitals.')
    });
  }

  private refreshVitals(): void {
    const c = this.consultation();
    if (!c) return;
    this.vitalsService.list(c.uid).subscribe({
      next: (vs) => this.vitals.set(vs),
      error: () => { /* keep existing */ }
    });
  }

  statusBadgeClass(s: ConsultationStatus): string {
    return 'badge ' + (this.statuses.find((x) => x.value === s)?.badgeClass ?? '');
  }

  statusLabel(s: ConsultationStatus): string {
    return this.statuses.find((x) => x.value === s)?.label ?? s;
  }

  formatBp(v: PatientVitals): string {
    if (v.bloodPressureSystolic == null || v.bloodPressureDiastolic == null) return '—';
    return `${v.bloodPressureSystolic} / ${v.bloodPressureDiastolic}`;
  }
}
