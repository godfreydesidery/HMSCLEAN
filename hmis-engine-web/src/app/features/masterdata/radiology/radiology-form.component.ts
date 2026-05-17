import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { RadiologyTypeService } from './radiology.service';
import { RADIOLOGY_MODALITIES, RadiologyModality, RadiologyType } from './radiology.types';

@Component({
  selector: 'app-radiology-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './radiology-form.component.html'
})
export class RadiologyFormComponent implements OnInit {
  @Input() existing: RadiologyType | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(RadiologyTypeService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly modalities = RADIOLOGY_MODALITIES;
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.maxLength(32), Validators.pattern(/^[A-Z0-9._-]+$/)]],
    name: ['', [Validators.required, Validators.maxLength(200)]],
    modality: ['X_RAY' as RadiologyModality, [Validators.required]],
    description: ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    if (this.existing) {
      const r = this.existing;
      this.form.patchValue({ code: r.code, name: r.name, modality: r.modality, description: r.description ?? '' });
      this.form.controls.code.disable();
    }
  }

  get isEdit(): boolean { return !!this.existing; }
  get title(): string { return this.isEdit ? 'Edit radiology type' : 'New radiology type'; }

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const payload = {
      name: raw.name.trim(),
      modality: raw.modality,
      description: raw.description?.trim() || null
    };
    const existing = this.existing;
    const req$ = existing
      ? this.service.update(existing.uid, payload)
      : this.service.create({ ...payload, code: raw.code.trim().toUpperCase() });
    req$.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (r) => this.activeModal.close(r),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save radiology type.')
    });
  }
}
