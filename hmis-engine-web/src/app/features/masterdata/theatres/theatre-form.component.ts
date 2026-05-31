import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { TheatreService } from './theatre.service';
import { Theatre } from './theatre.types';

@Component({
  selector: 'app-theatre-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './theatre-form.component.html'
})
export class TheatreFormComponent implements OnInit {
  @Input() existing: Theatre | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly theatreService = inject(TheatreService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(32), Validators.pattern(/^[A-Z0-9._-]+$/)]],
    name: ['', [Validators.required, Validators.maxLength(120)]],
    location: ['', [Validators.maxLength(80)]],
    description: ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    if (this.existing) {
      const t = this.existing;
      this.form.patchValue({
        code: t.code, name: t.name,
        location: t.location ?? '', description: t.description ?? ''
      });
      this.form.controls.code.disable();
    }
  }

  get isEdit(): boolean { return !!this.existing; }
  get title(): string { return this.isEdit ? 'Edit theatre' : 'New theatre'; }

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
      location: raw.location?.trim() || null,
      description: raw.description?.trim() || null
    };
    const existing = this.existing;
    const req$ = existing
      ? this.theatreService.update(existing.uid, payload)
      : this.theatreService.create({ ...payload, code: raw.code.trim().toUpperCase() });
    req$.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (t) => this.activeModal.close(t),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save theatre.')
    });
  }
}
