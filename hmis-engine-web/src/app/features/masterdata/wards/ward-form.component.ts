import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { WardService } from './ward.service';
import { WARD_CATEGORIES, Ward, WardCategory } from './ward.types';

@Component({
  selector: 'app-ward-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './ward-form.component.html'
})
export class WardFormComponent implements OnInit {
  @Input() existing: Ward | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly wardService = inject(WardService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly categories = WARD_CATEGORIES;
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.maxLength(32), Validators.pattern(/^[A-Z0-9_-]+$/)]],
    name: ['', [Validators.required, Validators.maxLength(120)]],
    category: ['GENERAL' as WardCategory, [Validators.required]],
    capacity: [0, [Validators.required, Validators.min(0)]],
    location: ['', [Validators.maxLength(80)]],
    description: ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    if (this.existing) {
      const w = this.existing;
      this.form.patchValue({
        code: w.code, name: w.name, category: w.category, capacity: w.capacity,
        location: w.location ?? '', description: w.description ?? ''
      });
      this.form.controls.code.disable();
    }
  }

  get isEdit(): boolean { return !!this.existing; }
  get title(): string { return this.isEdit ? 'Edit ward' : 'New ward'; }

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
      category: raw.category,
      capacity: Number(raw.capacity),
      location: raw.location?.trim() || null,
      description: raw.description?.trim() || null
    };
    const existing = this.existing;
    const req$ = existing
      ? this.wardService.update(existing.uid, payload)
      : this.wardService.create({ ...payload, code: raw.code.trim().toUpperCase() });
    req$.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (w) => this.activeModal.close(w),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save ward.')
    });
  }
}
