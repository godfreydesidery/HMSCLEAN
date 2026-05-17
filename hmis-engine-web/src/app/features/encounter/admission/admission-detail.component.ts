import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgbDropdownModule, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { PAYMENT_TYPES, PaymentType } from '../../patient/patient.types';
import { WardService } from '../../masterdata/wards/ward.service';
import { Ward } from '../../masterdata/wards/ward.types';
import { AdmissionService } from './admission.service';
import { ADMISSION_STATUSES, Admission, AdmissionStatus } from './admission.types';

@Component({
  selector: 'app-admission-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, NgbDropdownModule],
  templateUrl: './admission-detail.component.html'
})
export class AdmissionDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly admissionService = inject(AdmissionService);
  private readonly wardService = inject(WardService);
  private readonly modal = inject(NgbModal);
  private readonly fb = inject(FormBuilder);

  readonly statuses = ADMISSION_STATUSES;
  readonly paymentTypes = PAYMENT_TYPES;

  readonly admission = signal<Admission | null>(null);
  readonly wards = signal<Ward[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly actionMessage = signal<string | null>(null);

  readonly isActive = computed(() => this.admission()?.status === 'ADMITTED');

  readonly transferForm = this.fb.nonNullable.group({
    wardUid: ['', [Validators.required]],
    bedLabel: ['', [Validators.maxLength(32)]]
  });
  readonly dischargeForm = this.fb.nonNullable.group({
    summary: ['', [Validators.maxLength(1000)]]
  });
  readonly cancelForm = this.fb.nonNullable.group({
    reason: ['', [Validators.maxLength(255)]]
  });

  constructor() {
    const uid = this.route.snapshot.paramMap.get('uid');
    if (!uid) {
      this.errorMessage.set('Missing admission identifier.');
      this.loading.set(false);
      return;
    }
    this.load(uid);
    this.wardService.search({ active: true, size: 200, sort: 'name,asc' }).subscribe({
      next: (page) => this.wards.set(page.content)
    });
  }

  private load(uid: string): void {
    this.loading.set(true);
    this.admissionService.findByUid(uid).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (a) => this.admission.set(a),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load admission.')
    });
  }

  openTransfer(content: unknown): void {
    const a = this.admission();
    if (!a) return;
    this.transferForm.reset({ wardUid: a.wardUid, bedLabel: a.bedLabel ?? '' });
    this.modal.open(content, { centered: true });
  }

  confirmTransfer(modalRef: { dismiss: () => void }): void {
    const a = this.admission(); if (!a) return;
    if (this.transferForm.invalid) { this.transferForm.markAllAsTouched(); return; }
    const raw = this.transferForm.getRawValue();
    this.busy.set(true);
    this.admissionService.transferWard(a.uid, { wardUid: raw.wardUid, bedLabel: raw.bedLabel?.trim() || null })
      .pipe(finalize(() => this.busy.set(false))).subscribe({
        next: (updated) => { this.admission.set(updated); this.actionMessage.set('Ward updated.'); modalRef.dismiss(); },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Transfer failed.')
      });
  }

  openDischarge(content: unknown, kind: 'discharge' | 'deceased' | 'transferOut'): void {
    const a = this.admission(); if (!a) return;
    this.dischargeForm.reset({ summary: a.dischargeSummary ?? '' });
    this.modal.open(content, { centered: true }).result.then((action) => {
      if (action === kind) this.confirmDischarge(a, kind);
    }, () => {});
  }

  private confirmDischarge(a: Admission, kind: 'discharge' | 'deceased' | 'transferOut'): void {
    if (this.dischargeForm.invalid) return;
    const summary = this.dischargeForm.controls.summary.value.trim() || null;
    this.busy.set(true);
    const obs = kind === 'discharge' ? this.admissionService.discharge(a.uid, summary)
              : kind === 'deceased' ? this.admissionService.markDeceased(a.uid, summary)
              : this.admissionService.transferOut(a.uid, summary);
    obs.pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (updated) => { this.admission.set(updated); this.actionMessage.set('Admission closed.'); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Action failed.')
    });
  }

  openCancel(content: unknown): void {
    this.cancelForm.reset({ reason: '' });
    this.modal.open(content, { centered: true }).result.then((action) => {
      if (action === 'cancel') this.confirmCancel();
    }, () => {});
  }

  private confirmCancel(): void {
    const a = this.admission(); if (!a) return;
    const reason = this.cancelForm.controls.reason.value.trim() || null;
    this.busy.set(true);
    this.admissionService.cancel(a.uid, reason).pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (updated) => { this.admission.set(updated); this.actionMessage.set('Admission cancelled.'); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Cancel failed.')
    });
  }

  backToList(): void { void this.router.navigate(['/encounters', 'admissions']); }

  statusBadgeClass(s: AdmissionStatus): string {
    return 'badge ' + (this.statuses.find((x) => x.value === s)?.badgeClass ?? '');
  }
  statusLabel(s: AdmissionStatus): string {
    return this.statuses.find((x) => x.value === s)?.label ?? s;
  }
  paymentLabel(p: PaymentType): string {
    return this.paymentTypes.find((x) => x.value === p)?.label ?? p;
  }
  patientInitials(a: Admission): string {
    const parts = (a.patientName ?? '').split(' ').filter((p) => p.length > 0);
    if (parts.length === 0) return '?';
    const first = parts[0][0] ?? '';
    const last = parts.length > 1 ? parts[parts.length - 1][0] : '';
    return (first + last).toUpperCase();
  }
}
