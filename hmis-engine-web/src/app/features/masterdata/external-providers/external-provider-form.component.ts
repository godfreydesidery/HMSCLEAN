import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { ExternalProviderService } from './external-provider.service';
import { ExternalMedicalProvider } from './external-provider.types';

@Component({
  selector: 'app-external-provider-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './external-provider-form.component.html'
})
export class ExternalProviderFormComponent implements OnInit {
  @Input() existing: ExternalMedicalProvider | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ExternalProviderService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(32), Validators.pattern(/^[A-Z0-9._-]+$/)]],
    name: ['', [Validators.required, Validators.maxLength(200)]],
    address: ['', [Validators.maxLength(255)]],
    telephone: ['', [Validators.maxLength(40)]],
    email: ['', [Validators.maxLength(120), Validators.email]],
    fax: ['', [Validators.maxLength(40)]],
    website: ['', [Validators.maxLength(200)]]
  });

  ngOnInit(): void {
    if (this.existing) {
      const p = this.existing;
      this.form.patchValue({
        code: p.code, name: p.name, address: p.address ?? '', telephone: p.telephone ?? '',
        email: p.email ?? '', fax: p.fax ?? '', website: p.website ?? ''
      });
      this.form.controls.code.disable();
    }
  }

  get isEdit(): boolean { return !!this.existing; }
  get title(): string { return this.isEdit ? 'Edit external provider' : 'New external provider'; }

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const payload = {
      name: raw.name.trim(),
      address: raw.address?.trim() || null,
      telephone: raw.telephone?.trim() || null,
      email: raw.email?.trim() || null,
      fax: raw.fax?.trim() || null,
      website: raw.website?.trim() || null
    };
    const existing = this.existing;
    const req$ = existing
      ? this.service.update(existing.uid, payload)
      : this.service.create({ ...payload, code: raw.code.trim().toUpperCase() });
    req$.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (p) => this.activeModal.close(p),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save external provider.')
    });
  }
}
