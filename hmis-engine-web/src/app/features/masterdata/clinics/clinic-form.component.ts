import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { ClinicService } from './clinic.service';
import { CLINIC_TYPES, Clinic, ClinicType } from './clinic.types';

@Component({
  selector: 'app-clinic-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './clinic-form.component.html',
  styleUrl: './clinic-form.component.scss'
})
export class ClinicFormComponent implements OnInit {
  @Input() existing: Clinic | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly clinicService = inject(ClinicService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly clinicTypes = CLINIC_TYPES;

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.maxLength(32), Validators.pattern(/^[A-Z0-9_-]+$/)]],
    name: ['', [Validators.required, Validators.maxLength(120)]],
    type: ['OUTPATIENT' as ClinicType, [Validators.required]],
    description: ['', [Validators.maxLength(500)]],
    location: ['', [Validators.maxLength(80)]]
  });

  ngOnInit(): void {
    if (this.existing) {
      const e = this.existing;
      this.form.patchValue({
        code: e.code,
        name: e.name,
        type: e.type,
        description: e.description ?? '',
        location: e.location ?? ''
      });
      this.form.controls.code.disable();
    }
  }

  get isEdit(): boolean {
    return !!this.existing;
  }

  get title(): string {
    return this.isEdit ? 'Edit clinic' : 'New clinic';
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);

    const raw = this.form.getRawValue();
    const payload = {
      name: raw.name.trim(),
      type: raw.type,
      description: raw.description?.trim() || null,
      location: raw.location?.trim() || null
    };
    const existing = this.existing;

    const request$ = existing
      ? this.clinicService.update(existing.id, payload)
      : this.clinicService.create({ ...payload, code: raw.code.trim().toUpperCase() });

    request$.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (clinic) => this.activeModal.close(clinic),
      error: (err) => {
        const fieldErrors: { field: string; message: string }[] = err?.error?.errors ?? [];
        const fieldSummary = fieldErrors.map((fe) => `${fe.field}: ${fe.message}`).join('; ');
        const message =
          fieldSummary || err?.error?.message || 'Could not save clinic. Please try again.';
        this.errorMessage.set(message);
      }
    });
  }
}
