import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { DosageService } from './dosage.service';
import { Dosage } from './dosage.types';

@Component({
  selector: 'app-dosage-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './dosage-form.component.html'
})
export class DosageFormComponent implements OnInit {
  @Input() existing: Dosage | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(DosageService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.maxLength(32), Validators.pattern(/^[A-Z0-9._-]+$/)]],
    name: ['', [Validators.required, Validators.maxLength(120)]],
    description: ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    if (this.existing) {
      const d = this.existing;
      this.form.patchValue({ code: d.code, name: d.name, description: d.description ?? '' });
      this.form.controls.code.disable();
    }
  }

  get isEdit(): boolean { return !!this.existing; }
  get title(): string { return this.isEdit ? 'Edit dosage' : 'New dosage'; }

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const payload = { name: raw.name.trim(), description: raw.description?.trim() || null };
    const existing = this.existing;
    const req$ = existing
      ? this.service.update(existing.uid, payload)
      : this.service.create({ ...payload, code: raw.code.trim().toUpperCase() });
    req$.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (d) => this.activeModal.close(d),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save dosage.')
    });
  }
}
