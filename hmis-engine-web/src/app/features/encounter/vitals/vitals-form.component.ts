import { CommonModule } from '@angular/common';
import { Component, Input, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { VitalsService } from './vitals.service';

@Component({
  selector: 'app-vitals-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './vitals-form.component.html'
})
export class VitalsFormComponent {
  @Input({ required: true }) consultationUid!: string;

  private readonly fb = inject(FormBuilder);
  private readonly vitalsService = inject(VitalsService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.group({
    temperatureC:           [null as number | null, [Validators.min(25), Validators.max(45)]],
    pulseBpm:               [null as number | null, [Validators.min(20), Validators.max(250)]],
    respirationBpm:         [null as number | null, [Validators.min(5),  Validators.max(80)]],
    bloodPressureSystolic:  [null as number | null, [Validators.min(40), Validators.max(260)]],
    bloodPressureDiastolic: [null as number | null, [Validators.min(20), Validators.max(200)]],
    spo2Percent:            [null as number | null, [Validators.min(40), Validators.max(100)]],
    weightKg:               [null as number | null, [Validators.min(0.5), Validators.max(400)]],
    heightCm:               [null as number | null, [Validators.min(20), Validators.max(260)]],
    notes:                  ['' as string,           [Validators.maxLength(500)]]
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const v = this.form.value;
    this.vitalsService.record(this.consultationUid, {
      temperatureC: v.temperatureC ?? null,
      pulseBpm: v.pulseBpm ?? null,
      respirationBpm: v.respirationBpm ?? null,
      bloodPressureSystolic: v.bloodPressureSystolic ?? null,
      bloodPressureDiastolic: v.bloodPressureDiastolic ?? null,
      spo2Percent: v.spo2Percent ?? null,
      weightKg: v.weightKg ?? null,
      heightCm: v.heightCm ?? null,
      notes: (v.notes ?? '').trim() || null
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (vitals) => this.activeModal.close(vitals),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save vitals.')
    });
  }
}
