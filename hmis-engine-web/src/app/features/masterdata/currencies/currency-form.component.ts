import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { CurrencyService } from './currency.service';
import { Currency } from './currency.types';

@Component({
  selector: 'app-currency-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './currency-form.component.html'
})
export class CurrencyFormComponent implements OnInit {
  @Input() existing: Currency | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly currencyService = inject(CurrencyService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.maxLength(3), Validators.pattern(/^[A-Za-z]{3}$/)]],
    name: ['', [Validators.required, Validators.maxLength(80)]],
    symbol: ['', [Validators.maxLength(8)]],
    makeDefault: [false]
  });

  ngOnInit(): void {
    if (this.existing) {
      const c = this.existing;
      this.form.patchValue({ code: c.code, name: c.name, symbol: c.symbol ?? '' });
      this.form.controls.code.disable();
    }
  }

  get isEdit(): boolean { return !!this.existing; }
  get title(): string { return this.isEdit ? 'Edit currency' : 'New currency'; }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const existing = this.existing;
    const req$ = existing
      ? this.currencyService.update(existing.uid, { name: raw.name.trim(), symbol: raw.symbol?.trim() || null })
      : this.currencyService.create({
          code: raw.code.trim().toUpperCase(),
          name: raw.name.trim(),
          symbol: raw.symbol?.trim() || null,
          makeDefault: raw.makeDefault
        });
    req$.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (c) => this.activeModal.close(c),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save currency.')
    });
  }
}
