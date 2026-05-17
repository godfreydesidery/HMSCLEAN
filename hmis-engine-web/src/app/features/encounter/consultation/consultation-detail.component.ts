import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize, forkJoin } from 'rxjs';

import { AddDiagnosisComponent } from '../diagnosis/add-diagnosis.component';
import { ConsultationDiagnosisService } from '../diagnosis/consultation-diagnosis.service';
import {
  ConsultationDiagnosis, DIAGNOSIS_KINDS, DiagnosisKind
} from '../diagnosis/consultation-diagnosis.types';
import { ClinicalNoteService } from '../note/clinical-note.service';
import { ClinicalNote } from '../note/clinical-note.types';
import { VitalsFormComponent } from '../vitals/vitals-form.component';
import { VitalsService } from '../vitals/vitals.service';
import { PatientVitals } from '../vitals/vitals.types';
import { ConsultationService } from './consultation.service';
import { CONSULTATION_STATUSES, Consultation, ConsultationStatus } from './consultation.types';

type TabKey = 'overview' | 'vitals' | 'notes' | 'diagnoses' | 'orders';

@Component({
  selector: 'app-consultation-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './consultation-detail.component.html',
  styleUrl: './consultation-detail.component.scss'
})
export class ConsultationDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly consultationService = inject(ConsultationService);
  private readonly vitalsService = inject(VitalsService);
  private readonly noteService = inject(ClinicalNoteService);
  private readonly diagnosisService = inject(ConsultationDiagnosisService);
  private readonly modal = inject(NgbModal);
  private readonly fb = inject(FormBuilder);

  readonly statuses = CONSULTATION_STATUSES;
  readonly diagnosisKinds = DIAGNOSIS_KINDS;
  readonly consultation = signal<Consultation | null>(null);
  readonly vitals = signal<PatientVitals[]>([]);
  readonly diagnoses = signal<ConsultationDiagnosis[]>([]);
  readonly note = signal<ClinicalNote | null>(null);
  readonly noteDirty = signal(false);
  readonly notesSaving = signal(false);
  readonly notesSavedAt = signal<string | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly activeTab = signal<TabKey>('overview');

  readonly noteForm = this.fb.nonNullable.group({
    chiefComplaint: [''],
    historyOfPresentingIllness: [''],
    pastMedicalHistory: [''],
    examination: [''],
    assessment: [''],
    plan: ['']
  });

  readonly canStart = computed(() => this.consultation()?.status === 'BOOKED');
  readonly canComplete = computed(() => this.consultation()?.status === 'IN_PROGRESS');
  readonly canCancel = computed(() => {
    const s = this.consultation()?.status;
    return s === 'BOOKED' || s === 'IN_PROGRESS';
  });
  readonly isEditable = computed(() => {
    const s = this.consultation()?.status;
    return s === 'BOOKED' || s === 'IN_PROGRESS';
  });

  readonly workingDiagnoses = computed(() => this.diagnoses().filter((d) => d.kind === 'WORKING'));
  readonly finalDiagnoses = computed(() => this.diagnoses().filter((d) => d.kind === 'FINAL'));

  constructor() {
    const uid = this.route.snapshot.paramMap.get('uid');
    if (!uid) {
      this.loading.set(false);
      this.errorMessage.set('Missing consultation identifier.');
      return;
    }
    forkJoin({
      consultation: this.consultationService.findByUid(uid),
      vitals: this.vitalsService.list(uid),
      note: this.noteService.get(uid),
      diagnoses: this.diagnosisService.list(uid)
    }).subscribe({
      next: ({ consultation, vitals, note, diagnoses }) => {
        this.consultation.set(consultation);
        this.vitals.set(vitals);
        this.diagnoses.set(diagnoses);
        this.setNote(note);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.message ?? 'Could not load consultation.');
        this.loading.set(false);
      }
    });

    this.noteForm.valueChanges.subscribe(() => this.noteDirty.set(true));
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

  saveNote(): void {
    const c = this.consultation();
    if (!c || this.notesSaving()) return;
    this.notesSaving.set(true);
    this.errorMessage.set(null);
    const raw = this.noteForm.getRawValue();
    const payload = {
      chiefComplaint: emptyToNull(raw.chiefComplaint),
      historyOfPresentingIllness: emptyToNull(raw.historyOfPresentingIllness),
      pastMedicalHistory: emptyToNull(raw.pastMedicalHistory),
      examination: emptyToNull(raw.examination),
      assessment: emptyToNull(raw.assessment),
      plan: emptyToNull(raw.plan)
    };
    this.noteService.save(c.uid, payload)
      .pipe(finalize(() => this.notesSaving.set(false))).subscribe({
        next: (saved) => {
          this.setNote(saved);
          this.notesSavedAt.set(new Date().toLocaleTimeString());
        },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save clinical note.')
      });
  }

  addDiagnosis(initialKind: DiagnosisKind): void {
    const c = this.consultation();
    if (!c) return;
    const ref = this.modal.open(AddDiagnosisComponent, { size: 'lg', backdrop: 'static' });
    const inst = ref.componentInstance as AddDiagnosisComponent;
    inst.consultationUid = c.uid;
    inst.initialKind = initialKind;
    ref.closed.subscribe(() => this.refreshDiagnoses());
  }

  removeDiagnosis(d: ConsultationDiagnosis): void {
    if (!globalThis.confirm(`Remove diagnosis "${d.diagnosisName ?? d.diagnosisTypeUid}"?`)) return;
    const c = this.consultation();
    if (!c) return;
    this.diagnosisService.remove(c.uid, d.uid).subscribe({
      next: () => this.refreshDiagnoses(),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not remove diagnosis.')
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

  private refreshDiagnoses(): void {
    const c = this.consultation();
    if (!c) return;
    this.diagnosisService.list(c.uid).subscribe({
      next: (ds) => this.diagnoses.set(ds),
      error: () => { /* keep existing */ }
    });
  }

  private setNote(note: ClinicalNote | null): void {
    this.note.set(note);
    if (note) {
      this.noteForm.patchValue({
        chiefComplaint: note.chiefComplaint ?? '',
        historyOfPresentingIllness: note.historyOfPresentingIllness ?? '',
        pastMedicalHistory: note.pastMedicalHistory ?? '',
        examination: note.examination ?? '',
        assessment: note.assessment ?? '',
        plan: note.plan ?? ''
      }, { emitEvent: false });
    }
    this.noteDirty.set(false);
  }

  statusBadgeClass(s: ConsultationStatus): string {
    return 'badge ' + (this.statuses.find((x) => x.value === s)?.badgeClass ?? '');
  }

  statusLabel(s: ConsultationStatus): string {
    return this.statuses.find((x) => x.value === s)?.label ?? s;
  }

  diagnosisBadgeClass(k: DiagnosisKind): string {
    return 'badge ' + (this.diagnosisKinds.find((x) => x.value === k)?.badgeClass ?? '');
  }

  formatBp(v: PatientVitals): string {
    if (v.bloodPressureSystolic == null || v.bloodPressureDiastolic == null) return '—';
    return `${v.bloodPressureSystolic} / ${v.bloodPressureDiastolic}`;
  }
}

function emptyToNull(v: string | null | undefined): string | null {
  if (v == null) return null;
  const t = v.trim();
  return t === '' ? null : t;
}
