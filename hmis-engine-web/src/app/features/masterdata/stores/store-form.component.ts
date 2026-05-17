import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { StoreService } from './store.service';
import { Store } from './store.types';

@Component({
  selector: 'app-store-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './store-form.component.html'
})
export class StoreFormComponent implements OnInit {
  @Input() existing: Store | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly storeService = inject(StoreService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.maxLength(32), Validators.pattern(/^[A-Z0-9_-]+$/)]],
    name: ['', [Validators.required, Validators.maxLength(120)]],
    location: ['', [Validators.maxLength(80)]],
    description: ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    if (this.existing) {
      const s = this.existing;
      this.form.patchValue({ code: s.code, name: s.name, location: s.location ?? '', description: s.description ?? '' });
      this.form.controls.code.disable();
    }
  }

  get isEdit(): boolean { return !!this.existing; }
  get title(): string { return this.isEdit ? 'Edit store' : 'New store'; }

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const payload = {
      name: raw.name.trim(),
      location: raw.location?.trim() || null,
      description: raw.description?.trim() || null
    };
    const existing = this.existing;
    const req$ = existing
      ? this.storeService.update(existing.uid, payload)
      : this.storeService.create({ ...payload, code: raw.code.trim().toUpperCase() });
    req$.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (s) => this.activeModal.close(s),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save store.')
    });
  }
}
