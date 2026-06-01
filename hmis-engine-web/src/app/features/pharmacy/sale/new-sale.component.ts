import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';

import { WorkingLocationService } from '../../../core/working-location/working-location.service';
import { InsurancePlanService } from '../../masterdata/insurance-plans/insurance-plan.service';
import { InsurancePlan } from '../../masterdata/insurance-plans/insurance-plan.types';
import { MedicineService } from '../../masterdata/medicines/medicine.service';
import { Medicine } from '../../masterdata/medicines/medicine.types';
import { PAYMENT_TYPES, PaymentType } from '../../patient/patient.types';
import { PharmacySaleOrderService } from './pharmacy-sale.service';
import { AddLineRequest } from './pharmacy-sale.types';

interface CartLine extends AddLineRequest {
  medicineLabel: string;
}

@Component({
  selector: 'app-new-pharmacy-sale',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './new-sale.component.html'
})
export class NewSaleComponent {
  private readonly fb = inject(FormBuilder);
  private readonly saleService = inject(PharmacySaleOrderService);
  private readonly medicineService = inject(MedicineService);
  private readonly planService = inject(InsurancePlanService);
  private readonly workingLocation = inject(WorkingLocationService);
  private readonly router = inject(Router);

  readonly paymentTypes = PAYMENT_TYPES;

  /** The selling pharmacy = the operator's own working pharmacy (read-only, legacy). */
  readonly workingPharmacy = this.workingLocation.workingPharmacy;

  readonly medicines = signal<Medicine[]>([]);
  readonly plans = signal<InsurancePlan[]>([]);
  readonly loadingLookups = signal(true);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly cart = signal<CartLine[]>([]);
  readonly cartSubtotal = computed(() =>
    this.cart().reduce((sum, l) => sum + (l.quantity * l.unitPrice), 0));

  readonly form = this.fb.nonNullable.group({
    customerName: ['', [Validators.required, Validators.maxLength(160)]],
    customerPhone: ['', [Validators.maxLength(40)]],
    paymentType: ['CASH' as PaymentType, [Validators.required]],
    insurancePlanUid: ['']
  });

  readonly lineForm = this.fb.nonNullable.group({
    medicineUid: ['', [Validators.required]],
    quantity: [1, [Validators.required, Validators.min(1)]],
    dose: ['', [Validators.maxLength(80)]],
    frequency: ['', [Validators.maxLength(80)]],
    durationDays: [null as number | null, [Validators.min(0)]],
    instructions: ['', [Validators.maxLength(500)]],
    unitPrice: [0, [Validators.required, Validators.min(0)]]
  });

  constructor() {
    // The selling pharmacy comes only from the working location (legacy "selected
    // pharmacy"); with none set, gate the action behind the Select page.
    if (!this.workingPharmacy()) {
      void this.router.navigate(['/pharmacy/select']);
      return;
    }
    forkJoin({
      medicines: this.medicineService.search({ active: true, size: 500, sort: 'name,asc' }),
      plans: this.planService.search({ active: true, size: 200, sort: 'name,asc' })
    }).pipe(finalize(() => this.loadingLookups.set(false))).subscribe({
      next: ({ medicines, plans }) => {
        this.medicines.set(medicines.content);
        this.plans.set(plans.content);
      },
      error: () => this.errorMessage.set('Could not load lookups.')
    });
  }

  get insuranceRequired(): boolean {
    const pt = this.form.controls.paymentType.value;
    return pt === 'INSURANCE' || pt === 'MIXED';
  }

  medicineLabel(m: Medicine): string {
    const parts = [m.name];
    if (m.strength) parts.push(m.strength);
    if (m.form) parts.push(m.form.toLowerCase());
    return parts.join(' · ');
  }

  addLineToCart(): void {
    if (this.lineForm.invalid) { this.lineForm.markAllAsTouched(); return; }
    const raw = this.lineForm.getRawValue();
    const med = this.medicines().find((m) => m.uid === raw.medicineUid);
    if (!med) return;
    this.cart.update((arr) => [...arr, {
      medicineUid: raw.medicineUid,
      quantity: raw.quantity,
      dose: raw.dose?.trim() || null,
      frequency: raw.frequency?.trim() || null,
      durationDays: raw.durationDays ?? null,
      instructions: raw.instructions?.trim() || null,
      unitPrice: raw.unitPrice,
      medicineLabel: this.medicineLabel(med)
    }]);
    this.lineForm.reset({
      medicineUid: '', quantity: 1, dose: '', frequency: '', durationDays: null, instructions: '', unitPrice: 0
    });
  }

  removeCartLine(idx: number): void {
    this.cart.update((arr) => arr.filter((_, i) => i !== idx));
  }

  submit(): void {
    if (this.submitting()) return;
    const working = this.workingPharmacy();
    if (!working) { void this.router.navigate(['/pharmacy/select']); return; }
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    if (this.cart().length === 0) {
      this.errorMessage.set('Add at least one medicine to the cart.');
      return;
    }
    if (this.insuranceRequired && !this.form.controls.insurancePlanUid.value) {
      this.form.controls.insurancePlanUid.setErrors({ required: true });
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.saleService.create({
      pharmacyUid: working.uid,
      customerName: raw.customerName.trim(),
      customerPhone: raw.customerPhone?.trim() || null,
      patientUid: null,
      paymentType: raw.paymentType,
      insurancePlanUid: raw.insurancePlanUid || null,
      lines: this.cart().map(({ medicineLabel: _ml, ...line }) => line)
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (sale) => void this.router.navigate(['/pharmacy/sales', sale.uid]),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not open sale.')
    });
  }

  cancel(): void { void this.router.navigate(['/pharmacy/sales']); }
}
