import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { InsuranceProviderService } from '../insurance/insurance.service';
import { InsuranceProvider } from '../insurance/insurance.types';
import { InsurancePlanService } from './insurance-plan.service';
import { InsurancePlan } from './insurance-plan.types';

@Component({
  selector: 'app-insurance-plan-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './insurance-plan-form.component.html'
})
export class InsurancePlanFormComponent implements OnInit {
  @Input() existing: InsurancePlan | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly planService = inject(InsurancePlanService);
  private readonly providerService = inject(InsuranceProviderService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly providers = signal<InsuranceProvider[]>([]);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.maxLength(32), Validators.pattern(/^[A-Z0-9._-]+$/)]],
    name: ['', [Validators.required, Validators.maxLength(200)]],
    providerUid: ['', [Validators.required]],
    coversConsultation: [true],
    coversLab: [true],
    coversRadiology: [true],
    coversProcedure: [true],
    coversMedicine: [true],
    coversAdmission: [true],
    description: ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    // Load active providers for the dropdown
    this.providerService.search({ active: true, size: 100, sort: 'name,asc' }).subscribe({
      next: (res) => this.providers.set(res.content),
      error: () => this.errorMessage.set('Could not load insurance providers.')
    });

    if (this.existing) {
      const p = this.existing;
      this.form.patchValue({
        code: p.code, name: p.name, providerUid: p.providerUid,
        coversConsultation: p.coversConsultation, coversLab: p.coversLab,
        coversRadiology: p.coversRadiology, coversProcedure: p.coversProcedure,
        coversMedicine: p.coversMedicine, coversAdmission: p.coversAdmission,
        description: p.description ?? ''
      });
      this.form.controls.code.disable();
      this.form.controls.providerUid.disable();
    }
  }

  get isEdit(): boolean { return !!this.existing; }
  get title(): string { return this.isEdit ? 'Edit insurance plan' : 'New insurance plan'; }

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const payload = {
      name: raw.name.trim(),
      coversConsultation: raw.coversConsultation,
      coversLab: raw.coversLab,
      coversRadiology: raw.coversRadiology,
      coversProcedure: raw.coversProcedure,
      coversMedicine: raw.coversMedicine,
      coversAdmission: raw.coversAdmission,
      description: raw.description?.trim() || null
    };
    const existing = this.existing;
    const req$ = existing
      ? this.planService.update(existing.uid, payload)
      : this.planService.create({
          ...payload,
          code: raw.code.trim().toUpperCase(),
          providerUid: raw.providerUid
        });
    req$.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (p) => this.activeModal.close(p),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save plan.')
    });
  }
}
