import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { ProcedureTypeService } from './procedure.service';
import { ProcedureType } from './procedure.types';

@Component({
  selector: 'app-procedure-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './procedure-form.component.html'
})
export class ProcedureFormComponent implements OnInit {
  @Input() existing: ProcedureType | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ProcedureTypeService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.maxLength(32), Validators.pattern(/^[A-Z0-9._-]+$/)]],
    name: ['', [Validators.required, Validators.maxLength(200)]],
    description: ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    if (this.existing) {
      const p = this.existing;
      this.form.patchValue({ code: p.code, name: p.name, description: p.description ?? '' });
      this.form.controls.code.disable();
    }
  }

  get isEdit(): boolean { return !!this.existing; }
  get title(): string { return this.isEdit ? 'Edit procedure' : 'New procedure'; }

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
      next: (p) => this.activeModal.close(p),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save procedure.')
    });
  }
}
