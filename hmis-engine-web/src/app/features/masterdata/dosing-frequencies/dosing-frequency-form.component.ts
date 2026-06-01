import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { DosingFrequencyService } from './dosing-frequency.service';
import { DosingFrequency } from './dosing-frequency.types';

@Component({
  selector: 'app-dosing-frequency-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './dosing-frequency-form.component.html'
})
export class DosingFrequencyFormComponent implements OnInit {
  @Input() existing: DosingFrequency | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(DosingFrequencyService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.maxLength(32), Validators.pattern(/^[A-Z0-9._-]+$/)]],
    name: ['', [Validators.required, Validators.maxLength(120)]],
    timesPerDay: [null as number | null, [Validators.min(0)]],
    description: ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    if (this.existing) {
      const d = this.existing;
      this.form.patchValue({ code: d.code, name: d.name, timesPerDay: d.timesPerDay, description: d.description ?? '' });
      this.form.controls.code.disable();
    }
  }

  get isEdit(): boolean { return !!this.existing; }
  get title(): string { return this.isEdit ? 'Edit dosing frequency' : 'New dosing frequency'; }

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const timesPerDay = raw.timesPerDay === null || raw.timesPerDay === undefined || (raw.timesPerDay as unknown as string) === '' ? null : Number(raw.timesPerDay);
    const payload = { name: raw.name.trim(), timesPerDay, description: raw.description?.trim() || null };
    const existing = this.existing;
    const req$ = existing
      ? this.service.update(existing.uid, payload)
      : this.service.create({ ...payload, code: raw.code.trim().toUpperCase() });
    req$.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (d) => this.activeModal.close(d),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save dosing frequency.')
    });
  }
}
