import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { InsuranceProviderService } from './insurance.service';
import { InsuranceProvider } from './insurance.types';

@Component({
  selector: 'app-insurance-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './insurance-form.component.html'
})
export class InsuranceFormComponent implements OnInit {
  @Input() existing: InsuranceProvider | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(InsuranceProviderService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.maxLength(32), Validators.pattern(/^[A-Z0-9._-]+$/)]],
    name: ['', [Validators.required, Validators.maxLength(200)]],
    contactPerson: ['', [Validators.maxLength(120)]],
    phone: ['', [Validators.maxLength(40)]],
    email: ['', [Validators.email, Validators.maxLength(120)]],
    address: ['', [Validators.maxLength(255)]],
    description: ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    if (this.existing) {
      const i = this.existing;
      this.form.patchValue({
        code: i.code, name: i.name,
        contactPerson: i.contactPerson ?? '', phone: i.phone ?? '',
        email: i.email ?? '', address: i.address ?? '', description: i.description ?? ''
      });
      this.form.controls.code.disable();
    }
  }

  get isEdit(): boolean { return !!this.existing; }
  get title(): string { return this.isEdit ? 'Edit insurance provider' : 'New insurance provider'; }

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const payload = {
      name: raw.name.trim(),
      contactPerson: raw.contactPerson?.trim() || null,
      phone: raw.phone?.trim() || null,
      email: raw.email?.trim() || null,
      address: raw.address?.trim() || null,
      description: raw.description?.trim() || null
    };
    const existing = this.existing;
    const req$ = existing
      ? this.service.update(existing.uid, payload)
      : this.service.create({ ...payload, code: raw.code.trim().toUpperCase() });
    req$.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (i) => this.activeModal.close(i),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save insurance provider.')
    });
  }
}
