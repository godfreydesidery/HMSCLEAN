import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { finalize } from 'rxjs';

import { PatientFormComponent } from './patient-form.component';
import { PatientService } from './patient.service';
import { Patient } from './patient.types';

@Component({
  selector: 'app-patient-edit',
  standalone: true,
  imports: [CommonModule, PatientFormComponent],
  template: `
    @if (loading()) {
      <div class="hmis-empty"><i class="bi bi-hourglass-split"></i><div class="small">Loading patient…</div></div>
    } @else if (errorMessage()) {
      <div class="alert alert-danger">{{ errorMessage() }}</div>
    } @else {
      @if (patient(); as p) {
        <app-patient-form [existing]="p"></app-patient-form>
      }
    }
  `
})
export class PatientEditComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly patientService = inject(PatientService);

  readonly patient = signal<Patient | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  constructor() {
    const uid = this.route.snapshot.paramMap.get('uid');
    if (!uid) {
      this.loading.set(false);
      this.errorMessage.set('Missing patient identifier.');
      return;
    }
    this.patientService.findByUid(uid)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (p) => this.patient.set(p),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load patient.')
      });
  }
}
