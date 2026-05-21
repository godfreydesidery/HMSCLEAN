import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { StaffDirectoryService, StaffOption } from '../../../core/directory/staff-directory.service';
import { ClinicCliniciansService } from './clinic-clinicians.service';
import { ClinicClinician } from './clinic-clinicians.types';
import { Clinic } from './clinic.types';

/**
 * Clinic admin "Clinicians" panel: lists the clinicians affiliated with a
 * clinic and lets an admin add (from CLINICIAN-role users) or remove them.
 * Mirrors the legacy clinician multi-clinic selector, inverted to the clinic.
 */
@Component({
  selector: 'app-clinic-clinicians',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './clinic-clinicians.component.html'
})
export class ClinicCliniciansComponent implements OnInit {
  @Input({ required: true }) clinic!: Clinic;

  private readonly service = inject(ClinicCliniciansService);
  private readonly staffService = inject(StaffDirectoryService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly assigned = signal<ClinicClinician[]>([]);
  readonly allClinicians = signal<StaffOption[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly selectedUserUid = new FormControl('', { nonNullable: true });

  /** CLINICIAN-role users not already affiliated with this clinic. */
  readonly assignable = computed(() => {
    const taken = new Set(this.assigned().map((a) => a.userUid));
    return this.allClinicians().filter((s) => !taken.has(s.uid));
  });

  ngOnInit(): void {
    this.staffService.byRole('CLINICIAN').subscribe({
      next: (rows) => this.allClinicians.set(rows),
      error: () => this.errorMessage.set('Could not load clinician directory.')
    });
    this.reload();
  }

  private reload(): void {
    this.loading.set(true);
    this.service.list(this.clinic.uid)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (rows) => this.assigned.set(rows),
        error: () => this.errorMessage.set('Could not load assigned clinicians.')
      });
  }

  assign(): void {
    const userUid = this.selectedUserUid.value;
    if (!userUid || this.busy()) { return; }
    this.busy.set(true);
    this.errorMessage.set(null);
    this.service.assign(this.clinic.uid, userUid)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: () => { this.selectedUserUid.setValue(''); this.reload(); },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not assign clinician.')
      });
  }

  remove(row: ClinicClinician): void {
    if (this.busy()) { return; }
    this.busy.set(true);
    this.errorMessage.set(null);
    this.service.remove(this.clinic.uid, row.userUid)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: () => this.reload(),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not remove clinician.')
      });
  }
}
