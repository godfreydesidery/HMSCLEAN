import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { MedicineService } from './medicine.service';
import { MEDICINE_FORMS, Medicine, MedicineForm } from './medicine.types';

@Component({
  selector: 'app-medicine-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './medicine-form.component.html'
})
export class MedicineFormComponent implements OnInit {
  @Input() existing: Medicine | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(MedicineService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly forms = MEDICINE_FORMS;
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.maxLength(32), Validators.pattern(/^[A-Z0-9._-]+$/)]],
    name: ['', [Validators.required, Validators.maxLength(200)]],
    genericName: ['', [Validators.maxLength(200)]],
    strength: ['', [Validators.maxLength(80)]],
    medForm: ['TABLET' as MedicineForm, [Validators.required]],
    description: ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    if (this.existing) {
      const m = this.existing;
      this.form.patchValue({
        code: m.code, name: m.name,
        genericName: m.genericName ?? '', strength: m.strength ?? '',
        medForm: m.form, description: m.description ?? ''
      });
      this.form.controls.code.disable();
    }
  }

  get isEdit(): boolean { return !!this.existing; }
  get title(): string { return this.isEdit ? 'Edit medicine' : 'New medicine'; }

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const payload = {
      name: raw.name.trim(),
      genericName: raw.genericName?.trim() || null,
      strength: raw.strength?.trim() || null,
      form: raw.medForm,
      description: raw.description?.trim() || null
    };
    const existing = this.existing;
    const req$ = existing
      ? this.service.update(existing.uid, payload)
      : this.service.create({ ...payload, code: raw.code.trim().toUpperCase() });
    req$.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (m) => this.activeModal.close(m),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save medicine.')
    });
  }
}
