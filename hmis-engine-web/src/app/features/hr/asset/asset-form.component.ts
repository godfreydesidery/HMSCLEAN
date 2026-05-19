import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { AssetService } from './asset.service';
import { Asset, CreateAssetRequest } from './asset.types';

@Component({
  selector: 'app-asset-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './asset-form.component.html'
})
export class AssetFormComponent implements OnInit {
  /** Pass when editing — undefined for create. */
  readonly existing = input<Asset | null>(null);

  private readonly fb = inject(FormBuilder);
  private readonly assetService = inject(AssetService);
  private readonly router = inject(Router);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    tag: ['', [Validators.required, Validators.maxLength(64)]],
    name: ['', [Validators.required, Validators.maxLength(160)]],
    category: ['', [Validators.maxLength(80)]],
    location: ['', [Validators.maxLength(120)]],
    description: ['', [Validators.maxLength(500)]],
    serialNo: ['', [Validators.maxLength(120)]],
    manufacturer: ['', [Validators.maxLength(120)]],
    model: ['', [Validators.maxLength(120)]],
    acquisitionDate: [''],
    acquisitionCost: ['', [Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
    currency: ['TZS', [Validators.pattern(/^[A-Z]{3}$/)]],
    custodianUsername: ['', [Validators.maxLength(64)]]
  });

  readonly isEdit = computed(() => !!this.existing());
  get title(): string { return this.isEdit() ? 'Edit asset' : 'Register asset'; }

  ngOnInit(): void {
    const e = this.existing();
    if (e) {
      this.form.controls.tag.disable(); // tag is immutable after creation
      this.form.patchValue({
        tag: e.tag,
        name: e.name,
        category: e.category ?? '',
        location: e.location ?? '',
        description: e.description ?? '',
        serialNo: e.serialNo ?? '',
        manufacturer: e.manufacturer ?? '',
        model: e.model ?? '',
        acquisitionDate: e.acquisitionDate ?? '',
        acquisitionCost: e.acquisitionCost ?? '',
        currency: e.currency ?? 'TZS',
        custodianUsername: e.custodianUsername ?? ''
      });
    }
  }

  submit(): void {
    if (this.submitting()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);

    const raw = this.form.getRawValue();
    const payload: CreateAssetRequest = {
      tag: raw.tag.trim(),
      name: raw.name.trim(),
      category: emptyToNull(raw.category),
      location: emptyToNull(raw.location),
      description: emptyToNull(raw.description),
      serialNo: emptyToNull(raw.serialNo),
      manufacturer: emptyToNull(raw.manufacturer),
      model: emptyToNull(raw.model),
      acquisitionDate: emptyToNull(raw.acquisitionDate),
      acquisitionCost: emptyToNull(raw.acquisitionCost),
      currency: emptyToNull(raw.currency),
      custodianUsername: emptyToNull(raw.custodianUsername)
    };

    const existing = this.existing();
    const { tag: _droppedTag, ...updatePayload } = payload;  // tag is immutable on update
    void _droppedTag;
    const req$ = existing
      ? this.assetService.update(existing.uid, updatePayload)
      : this.assetService.create(payload);

    req$.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (saved) => void this.router.navigate(['/hr/assets', saved.uid]),
      error: (err) => {
        const fieldErrors: { field: string; message: string }[] = err?.error?.errors ?? [];
        const summary = fieldErrors.map((fe) => `${fe.field}: ${fe.message}`).join('; ');
        this.errorMessage.set(summary || err?.error?.message || 'Could not save asset.');
      }
    });
  }

  cancel(): void {
    void this.router.navigate(['/hr/assets']);
  }
}

function emptyToNull(v: string | null | undefined): string | null {
  if (v == null) return null;
  const t = v.trim();
  return t === '' ? null : t;
}
