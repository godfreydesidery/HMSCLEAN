import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { LabTestTypeService } from './lab-test.service';
import { LabTestType } from './lab-test.types';

@Component({
  selector: 'app-lab-test-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './lab-test-form.component.html'
})
export class LabTestFormComponent implements OnInit {
  @Input() existing: LabTestType | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(LabTestTypeService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.maxLength(32), Validators.pattern(/^[A-Z0-9._-]+$/)]],
    name: ['', [Validators.required, Validators.maxLength(200)]],
    specimen: ['', [Validators.maxLength(80)]],
    unit: ['', [Validators.maxLength(32)]],
    description: ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    if (this.existing) {
      const l = this.existing;
      this.form.patchValue({
        code: l.code, name: l.name,
        specimen: l.specimen ?? '', unit: l.unit ?? '', description: l.description ?? ''
      });
      this.form.controls.code.disable();
    }
  }

  get isEdit(): boolean { return !!this.existing; }
  get title(): string { return this.isEdit ? 'Edit lab test' : 'New lab test'; }

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const payload = {
      name: raw.name.trim(),
      specimen: raw.specimen?.trim() || null,
      unit: raw.unit?.trim() || null,
      description: raw.description?.trim() || null
    };
    const existing = this.existing;
    const req$ = existing
      ? this.service.update(existing.uid, payload)
      : this.service.create({ ...payload, code: raw.code.trim().toUpperCase() });
    req$.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (l) => this.activeModal.close(l),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save lab test.')
    });
  }
}
