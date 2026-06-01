import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { WorkingLocationService } from '../../../../core/working-location/working-location.service';
import { RnService } from './rn.service';
import { CreateRNLineRequest, RNDto, ToPickDetail, ToPickSummary } from './rn.types';

@Component({
  selector: 'app-rn-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './rn-create.component.html'
})
export class RnCreateComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly rnService = inject(RnService);
  private readonly workingLocation = inject(WorkingLocationService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  /** The receiving location = the operator's own working pharmacy (read-only, legacy). */
  readonly workingPharmacy = this.workingLocation.workingPharmacy;

  /** Issued TOs the pharmacy can receive against (picker, when no ?toUid). */
  readonly tos = signal<ToPickSummary[]>([]);
  readonly loadingTos = signal(false);
  /** The TO selected/prefilled, with its lines. */
  readonly to = signal<ToPickDetail | null>(null);
  readonly loadingTo = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.group({
    receivingDate: this.fb.nonNullable.control(this.today()),
    note: this.fb.nonNullable.control('', [Validators.maxLength(500)]),
    lines: this.fb.array([] as FormGroup[])
  });

  /** True once a source TO has been chosen and its lines loaded. */
  readonly hasTo = computed(() => this.to() !== null);

  get linesArray(): FormArray<FormGroup> { return this.form.get('lines') as FormArray<FormGroup>; }

  ngOnInit(): void {
    // The receiving pharmacy is the operator's own working pharmacy; enforce the workspace.
    if (!this.workingPharmacy()) {
      void this.router.navigate(['/pharmacy/select']);
      return;
    }
    const toUid = this.route.snapshot.queryParamMap.get('toUid');
    if (toUid) {
      this.loadTo(toUid);
    } else {
      this.loadIssuedTos();
    }
  }

  private today(): string {
    const d = new Date();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${d.getFullYear()}-${mm}-${dd}`;
  }

  private loadIssuedTos(): void {
    this.loadingTos.set(true);
    this.rnService.searchTos({ size: 200, sort: 'createdAt,desc' })
      .pipe(finalize(() => this.loadingTos.set(false)))
      .subscribe({
        next: (page) => this.tos.set(page.content),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load issued transfer orders.')
      });
  }

  /** Picker selection → load the chosen TO and build the line form. */
  pickTo(uid: string): void {
    if (!uid) return;
    this.loadTo(uid);
  }

  private loadTo(uid: string): void {
    this.loadingTo.set(true);
    this.errorMessage.set(null);
    this.rnService.getTo(uid)
      .pipe(finalize(() => this.loadingTo.set(false)))
      .subscribe({
        next: (to) => { this.to.set(to); this.buildLines(to); },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load the transfer order.')
      });
  }

  private buildLines(to: ToPickDetail): void {
    this.linesArray.clear();
    for (const line of to.lines) {
      this.linesArray.push(this.fb.group({
        toLineUid: this.fb.nonNullable.control(line.uid),
        medicineName: this.fb.nonNullable.control(line.medicineName ?? line.uid),
        issuedQuantity: this.fb.nonNullable.control(line.issuedQuantity),
        receivedQuantity: this.fb.nonNullable.control(line.issuedQuantity, [Validators.required, Validators.min(0)])
      }));
    }
  }

  /** Reset the picker so a different TO can be selected. */
  changeTo(): void {
    this.to.set(null);
    this.linesArray.clear();
    this.errorMessage.set(null);
    if (this.tos().length === 0) this.loadIssuedTos();
  }

  shortfall(fg: FormGroup): number {
    const issued = Number(fg.controls['issuedQuantity'].value) || 0;
    const received = Number(fg.controls['receivedQuantity'].value);
    return Math.max(0, issued - (Number.isFinite(received) ? received : 0));
  }

  submit(): void {
    if (this.submitting()) return;
    const to = this.to();
    if (!to) { this.errorMessage.set('Select an issued transfer order to receive against.'); return; }
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    const lines: CreateRNLineRequest[] = this.linesArray.controls.map((fg) => ({
      toLineUid: fg.controls['toLineUid'].value as string,
      receivedQuantity: Number(fg.controls['receivedQuantity'].value) || 0
    }));
    if (lines.length === 0) { this.errorMessage.set('This transfer order has no lines to receive.'); return; }

    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.rnService.create({
      toUid: to.uid,
      receivingDate: raw.receivingDate || null,
      note: raw.note?.trim() || null,
      lines
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (rn: RNDto) => { void this.router.navigate(['..', rn.uid], { relativeTo: this.route }); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not create the receive note.')
    });
  }

  cancel(): void { void this.router.navigate(['..'], { relativeTo: this.route }); }
}
