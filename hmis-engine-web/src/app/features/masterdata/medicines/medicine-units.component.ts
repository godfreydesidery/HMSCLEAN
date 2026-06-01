import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, finalize } from 'rxjs';

import { MedicineService } from './medicine.service';
import { MedicineUnit } from './medicine.types';

/**
 * Per-medicine unit management (opened as a modal from the medicine list). Lists the
 * medicine's dispensing units and their conversion factor to the base unit, and lets
 * staff add / edit / delete units and flip a unit active/inactive. The BASE unit
 * (factorToBase = 1) is the anchor every other unit converts against — it cannot be
 * edited, deactivated or deleted; its controls are disabled and a "Base" badge shown.
 */
@Component({
  selector: 'app-medicine-units',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './medicine-units.component.html'
})
export class MedicineUnitsComponent implements OnInit {
  @Input({ required: true }) medicineUid!: string;
  @Input() medicineName = '';

  private readonly fb = inject(FormBuilder);
  private readonly medicineService = inject(MedicineService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly units = signal<MedicineUnit[]>([]);
  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  /** Set while editing an existing unit; null when the form creates a new one. */
  readonly editing = signal<MedicineUnit | null>(null);

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.maxLength(16), Validators.pattern(/^[A-Z0-9._-]+$/)]],
    name: ['', [Validators.required, Validators.maxLength(80)]],
    factorToBase: [null as number | null, [Validators.required, Validators.min(2)]]
  });

  ngOnInit(): void { this.load(); }

  private load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.medicineService.listUnits(this.medicineUid)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (rows) => this.units.set(rows),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load units.')
      });
  }

  startEdit(u: MedicineUnit): void {
    if (u.base) { return; }
    this.editing.set(u);
    // Code is immutable on edit; min for factorToBase relaxes to 1 on update.
    this.form.get('code')?.disable();
    this.form.get('factorToBase')?.setValidators([Validators.required, Validators.min(1)]);
    this.form.get('factorToBase')?.updateValueAndValidity();
    this.form.setValue({ code: u.code, name: u.name, factorToBase: u.factorToBase });
  }

  cancelEdit(): void {
    this.editing.set(null);
    this.form.get('code')?.enable();
    this.form.get('factorToBase')?.setValidators([Validators.required, Validators.min(2)]);
    this.form.get('factorToBase')?.updateValueAndValidity();
    this.form.reset({ code: '', name: '', factorToBase: null });
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const editing = this.editing();
    const req$ = editing
      ? this.medicineService.updateUnit(this.medicineUid, editing.uid, {
          name: raw.name.trim(),
          factorToBase: Number(raw.factorToBase)
        })
      : this.medicineService.createUnit(this.medicineUid, {
          code: raw.code.trim().toUpperCase(),
          name: raw.name.trim(),
          factorToBase: Number(raw.factorToBase)
        });
    req$.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: () => { this.cancelEdit(); this.load(); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save the unit.')
    });
  }

  toggleActive(u: MedicineUnit): void {
    if (u.base) { return; }
    this.act(this.medicineService.setUnitActive(this.medicineUid, u.uid, !u.active));
  }

  delete(u: MedicineUnit): void {
    if (u.base) { return; }
    if (!globalThis.confirm(`Delete unit "${u.code}"? This cannot be undone.`)) return;
    this.act(this.medicineService.deleteUnit(this.medicineUid, u.uid));
  }

  private act(req$: Observable<unknown>): void {
    this.errorMessage.set(null);
    req$.subscribe({
      next: () => this.load(),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'The action failed.')
    });
  }
}
