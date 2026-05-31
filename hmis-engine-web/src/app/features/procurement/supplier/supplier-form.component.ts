import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { SupplierService } from './supplier.service';
import { Supplier } from './supplier.types';

@Component({
  selector: 'app-supplier-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './supplier-form.component.html'
})
export class SupplierFormComponent implements OnInit {
  @Input() existing: Supplier | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly supplierService = inject(SupplierService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.maxLength(32)]],
    name: ['', [Validators.required, Validators.maxLength(200)]],
    contactName: ['', [Validators.maxLength(120)]],
    phone: ['', [Validators.maxLength(32)]],
    email: ['', [Validators.email, Validators.maxLength(120)]],
    address: ['', [Validators.maxLength(255)]],
    taxId: ['', [Validators.maxLength(64)]],
    notes: ['', [Validators.maxLength(500)]],
    vrn: ['', [Validators.maxLength(32)]],
    termsOfContract: ['', [Validators.maxLength(1000)]],
    bankName: ['', [Validators.maxLength(120)]],
    bankAccountName: ['', [Validators.maxLength(200)]],
    bankAccountNo: ['', [Validators.maxLength(64)]]
  });

  get isEdit(): boolean { return this.existing != null; }
  get title(): string { return this.isEdit ? 'Edit supplier' : 'Add supplier'; }

  ngOnInit(): void {
    if (this.existing) {
      this.form.patchValue({
        code: this.existing.code,
        name: this.existing.name,
        contactName: this.existing.contactName ?? '',
        phone: this.existing.phone ?? '',
        email: this.existing.email ?? '',
        address: this.existing.address ?? '',
        taxId: this.existing.taxId ?? '',
        notes: this.existing.notes ?? '',
        vrn: this.existing.vrn ?? '',
        termsOfContract: this.existing.termsOfContract ?? '',
        bankName: this.existing.bankName ?? '',
        bankAccountName: this.existing.bankAccountName ?? '',
        bankAccountNo: this.existing.bankAccountNo ?? ''
      });
      this.form.controls.code.disable();
    }
  }

  submit(): void {
    if (this.submitting()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const payload = {
      name: raw.name.trim(),
      contactName: raw.contactName?.trim() || null,
      phone: raw.phone?.trim() || null,
      email: raw.email?.trim() || null,
      address: raw.address?.trim() || null,
      taxId: raw.taxId?.trim() || null,
      notes: raw.notes?.trim() || null,
      vrn: raw.vrn?.trim() || null,
      termsOfContract: raw.termsOfContract?.trim() || null,
      bankName: raw.bankName?.trim() || null,
      bankAccountName: raw.bankAccountName?.trim() || null,
      bankAccountNo: raw.bankAccountNo?.trim() || null
    };
    const obs = this.isEdit
      ? this.supplierService.update(this.existing!.uid, payload)
      : this.supplierService.create({ ...payload, code: raw.code.trim() });
    obs.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (s) => this.activeModal.close(s),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save supplier.')
    });
  }
}
