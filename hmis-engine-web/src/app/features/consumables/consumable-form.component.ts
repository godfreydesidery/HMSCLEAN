import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { ConsumableMasterdataService } from './consumable.service';
import { Consumable } from './consumable.types';

@Component({
  selector: 'app-consumable-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './consumable-form.component.html'
})
export class ConsumableFormComponent implements OnInit {
  @Input() existing: Consumable | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ConsumableMasterdataService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.maxLength(32), Validators.pattern(/^[A-Z0-9._-]+$/)]],
    name: ['', [Validators.required, Validators.maxLength(160)]],
    unitOfMeasure: ['', [Validators.maxLength(32)]],
    description: ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    if (this.existing) {
      const c = this.existing;
      this.form.patchValue({
        code: c.code, name: c.name,
        unitOfMeasure: c.unitOfMeasure ?? '', description: c.description ?? ''
      });
      this.form.controls.code.disable();
    }
  }

  get isEdit(): boolean { return !!this.existing; }
  get title(): string { return this.isEdit ? 'Edit consumable' : 'New consumable'; }

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const payload = {
      name: raw.name.trim(),
      unitOfMeasure: raw.unitOfMeasure?.trim() || null,
      description: raw.description?.trim() || null
    };
    const existing = this.existing;
    const req$ = existing
      ? this.service.update(existing.uid, payload)
      : this.service.create({ ...payload, code: raw.code.trim().toUpperCase() });
    req$.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (c) => this.activeModal.close(c),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save consumable.')
    });
  }
}
