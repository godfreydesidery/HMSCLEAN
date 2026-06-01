import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';

import { WorkingLocationService } from '../../../../core/working-location/working-location.service';
import { TransferDocStatus, transferDocBadgeClass, transferDocLabel } from '../../transfer-common.types';
import { PpTOService } from './pp-to.service';
import { CreateTOLineRequest, RoPickDetail, RoPickSummary } from './pp-to.types';

/** RO statuses the delivering pharmacy can issue against. */
const ISSUABLE_RO_STATUSES: TransferDocStatus[] = ['APPROVED', 'SUBMITTED', 'IN_PROCESS'];

@Component({
  selector: 'app-pp-to-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './pp-to-create.component.html'
})
export class PpToCreateComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly toService = inject(PpTOService);
  private readonly workingLocation = inject(WorkingLocationService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  /** The issuing (delivering) location = the operator's own working pharmacy (read-only, legacy). */
  readonly workingPharmacy = this.workingLocation.workingPharmacy;

  /** Candidate ROs for the picker (when no roUid query param was supplied). */
  readonly ros = signal<RoPickSummary[]>([]);
  readonly loadingRos = signal(false);
  /** The selected/prefill RO detail. */
  readonly ro = signal<RoPickDetail | null>(null);
  readonly loadingRo = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    note: ['', [Validators.maxLength(500)]],
    lines: this.fb.array<FormGroup>([])
  });

  get lines(): FormArray<FormGroup> { return this.form.controls.lines; }

  /** Lines with a positive issue quantity exist → submit is meaningful. */
  readonly hasIssuableLines = computed(() => this.ro()?.lines.some((l) => l.outstandingQuantity > 0) ?? false);

  ngOnInit(): void {
    // The issuing (delivering) pharmacy is the operator's own working pharmacy; enforce the workspace.
    if (!this.workingPharmacy()) {
      void this.router.navigate(['/pharmacy/select']);
      return;
    }
    const roUid = this.route.snapshot.queryParamMap.get('roUid');
    if (roUid) {
      this.loadRo(roUid);
    } else {
      this.loadRoPicker();
    }
  }

  /** Load the list of ROs the delivering pharmacy can issue against (APPROVED/SUBMITTED/IN_PROCESS). */
  private loadRoPicker(): void {
    this.loadingRos.set(true);
    this.errorMessage.set(null);
    // The search endpoint takes a single status; fetch the issuable ones and merge.
    forkJoin(
      ISSUABLE_RO_STATUSES.map((status) =>
        this.toService.searchRos({ status, size: 100, sort: 'createdAt,desc' })
      )
    ).pipe(finalize(() => this.loadingRos.set(false))).subscribe({
      next: (pages) => {
        const seen = new Set<string>();
        const merged: RoPickSummary[] = [];
        for (const page of pages) {
          for (const r of page.content) {
            if (!seen.has(r.uid)) { seen.add(r.uid); merged.push(r); }
          }
        }
        merged.sort((a, b) => b.createdAt.localeCompare(a.createdAt));
        this.ros.set(merged);
      },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load requisitions.')
    });
  }

  /** Pick an RO (from the picker) and prefill the line table. */
  selectRo(uid: string): void { this.loadRo(uid); }

  private loadRo(uid: string): void {
    this.loadingRo.set(true);
    this.errorMessage.set(null);
    this.toService.getRo(uid).pipe(finalize(() => this.loadingRo.set(false))).subscribe({
      next: (ro) => { this.ro.set(ro); this.buildLines(ro); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load requisition.')
    });
  }

  /** Build a FormArray prefilled from the RO lines, defaulting issue qty to outstanding. */
  private buildLines(ro: RoPickDetail): void {
    this.lines.clear();
    for (const l of ro.lines) {
      const max = l.outstandingQuantity;
      this.lines.push(this.fb.nonNullable.group({
        roLineUid: [l.uid],
        medicineName: [l.medicineName ?? l.medicineUid],
        medicineCode: [l.medicineCode ?? ''],
        medicineStrength: [l.medicineStrength ?? ''],
        unitCode: [l.unitCode ?? ''],
        requestedQuantity: [l.requestedQuantity],
        outstandingQuantity: [max],
        quantity: [
          max,
          max > 0
            ? [Validators.required, Validators.min(1), Validators.max(max)]
            : []
        ]
      }));
    }
  }

  /** Reset back to the picker (clears the chosen RO). */
  changeRo(): void {
    this.ro.set(null);
    this.lines.clear();
    if (this.ros().length === 0) this.loadRoPicker();
  }

  submit(): void {
    if (this.submitting()) return;
    const ro = this.ro();
    if (!ro) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    // Only ship lines with a positive quantity.
    const lines: CreateTOLineRequest[] = this.lines.controls
      .map((g) => g.getRawValue() as { roLineUid: string; quantity: number; outstandingQuantity: number })
      .filter((v) => v.outstandingQuantity > 0 && v.quantity > 0)
      .map((v) => ({ roLineUid: v.roLineUid, quantity: v.quantity }));

    if (lines.length === 0) {
      this.errorMessage.set('Enter an issue quantity on at least one line.');
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);
    const note = this.form.controls.note.value?.trim();
    this.toService.create({ roUid: ro.uid, note: note || null, lines })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (to) => void this.router.navigate(['..', to.uid], { relativeTo: this.route }),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not create transfer order.')
      });
  }

  cancel(): void { void this.router.navigate(['..'], { relativeTo: this.route }); }

  statusBadgeClass(s: TransferDocStatus): string { return transferDocBadgeClass(s); }
  statusLabel(s: TransferDocStatus): string { return transferDocLabel(s); }
}
