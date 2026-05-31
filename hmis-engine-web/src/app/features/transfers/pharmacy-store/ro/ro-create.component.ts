import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';

import { MedicineService } from '../../../masterdata/medicines/medicine.service';
import { Medicine, MedicineUnit } from '../../../masterdata/medicines/medicine.types';
import { PharmacyService } from '../../../masterdata/pharmacies/pharmacy.service';
import { Pharmacy } from '../../../masterdata/pharmacies/pharmacy.types';
import { StoreService } from '../../../masterdata/stores/store.service';
import { Store } from '../../../masterdata/stores/store.types';
import { RoService } from './ro.service';
import { CreateROLineRequest, RODto } from './ro.types';

@Component({
  selector: 'app-ro-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './ro-create.component.html'
})
export class RoCreateComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly roService = inject(RoService);
  private readonly pharmacyService = inject(PharmacyService);
  private readonly storeService = inject(StoreService);
  private readonly medicineService = inject(MedicineService);
  private readonly router = inject(Router);

  readonly pharmacies = signal<Pharmacy[]>([]);
  readonly stores = signal<Store[]>([]);
  readonly medicines = signal<Medicine[]>([]);
  /** Units loaded per medicineUid, used to populate the unit picker once a medicine is chosen. */
  readonly unitsByMedicine = signal<Record<string, MedicineUnit[]>>({});
  readonly loadingLookups = signal(true);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    pharmacyUid: ['', [Validators.required]],
    storeUid: ['', [Validators.required]],
    validUntil: [''],
    note: ['', [Validators.maxLength(500)]],
    lines: this.fb.array<FormGroup>([])
  });

  get lines(): FormArray<FormGroup> {
    return this.form.get('lines') as FormArray<FormGroup>;
  }

  ngOnInit(): void {
    forkJoin({
      pharmacies: this.pharmacyService.search({ active: true, size: 200, sort: 'name,asc' }),
      stores: this.storeService.search({ active: true, size: 200, sort: 'name,asc' }),
      medicines: this.medicineService.search({ active: true, size: 300, sort: 'name,asc' })
    }).pipe(finalize(() => this.loadingLookups.set(false))).subscribe({
      next: ({ pharmacies, stores, medicines }) => {
        this.pharmacies.set(pharmacies.content);
        this.stores.set(stores.content);
        this.medicines.set(medicines.content);
        this.addLine();
      },
      error: () => this.errorMessage.set('Could not load lookups.')
    });
  }

  newLineGroup(): FormGroup {
    return this.fb.nonNullable.group({
      medicineUid: ['', [Validators.required]],
      unitUid: [''],
      quantity: [1, [Validators.required, Validators.min(1)]],
      note: ['', [Validators.maxLength(500)]]
    });
  }

  addLine(): void { this.lines.push(this.newLineGroup()); }
  removeLine(i: number): void { this.lines.removeAt(i); }

  /** When a medicine is picked, load its units (if not already cached) and reset the line's unit. */
  onMedicineChange(group: FormGroup): void {
    const medicineUid = group.get('medicineUid')!.value as string;
    group.get('unitUid')!.setValue('');
    if (!medicineUid || this.unitsByMedicine()[medicineUid]) return;
    this.medicineService.listUnits(medicineUid).subscribe({
      next: (units) => this.unitsByMedicine.update((m) => ({ ...m, [medicineUid]: units.filter((u) => u.active) })),
      error: () => this.unitsByMedicine.update((m) => ({ ...m, [medicineUid]: [] }))
    });
  }

  unitsFor(group: FormGroup): MedicineUnit[] {
    const medicineUid = group.get('medicineUid')!.value as string;
    return this.unitsByMedicine()[medicineUid] ?? [];
  }

  submit(): void {
    if (this.submitting()) return;
    if (this.form.invalid || this.lines.length === 0) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const lines: CreateROLineRequest[] = this.lines.controls.map((g) => {
      const v = g.getRawValue() as { medicineUid: string; unitUid: string; quantity: number; note: string };
      return {
        medicineUid: v.medicineUid,
        unitUid: v.unitUid || null,
        quantity: v.quantity,
        note: v.note?.trim() || null
      };
    });
    this.roService.create({
      pharmacyUid: raw.pharmacyUid,
      storeUid: raw.storeUid,
      validUntil: raw.validUntil || null,
      note: raw.note?.trim() || null,
      lines
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (ro: RODto) => void this.router.navigate(['/transfers/ro', ro.uid]),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not create requisition.')
    });
  }

  cancel(): void { void this.router.navigate(['/transfers/ro']); }
}
