import { CommonModule } from '@angular/common';
import { Component, Input, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { MedicineService } from '../../masterdata/medicines/medicine.service';
import { MedicineUnit } from '../../masterdata/medicines/medicine.types';
import { PurchaseOrderService } from './purchase-order.service';
import { GoodsReceipt, PurchaseOrder, PurchaseOrderLine } from './purchase-order.types';

@Component({
  selector: 'app-receive-goods',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './receive-goods.component.html'
})
export class ReceiveGoodsComponent {
  @Input({ required: true }) set order(po: PurchaseOrder) {
    this.po.set(po);
    this.buildLineForms(po.lines);
  }

  private readonly fb = inject(FormBuilder);
  private readonly purchaseOrderService = inject(PurchaseOrderService);
  private readonly medicineService = inject(MedicineService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly po = signal<PurchaseOrder | null>(null);
  readonly lineForms = signal<FormGroup[]>([]);
  /** Keyed by poLine.uid → active units for that line's medicine. */
  readonly unitsByLine = signal<Record<string, MedicineUnit[]>>({});
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    deliveryNote: ['', [Validators.maxLength(120)]],
    notes: ['', [Validators.maxLength(500)]]
  });

  private buildLineForms(lines: PurchaseOrderLine[]): void {
    const open = lines.filter((l) => l.outstandingQuantity > 0);
    const forms = open.map((line) => this.fb.nonNullable.group({
      poLineUid: [line.uid],
      medicineUid: [line.medicineUid],
      medicineLabel: [(line.medicineCode ?? '') + ' — ' + (line.medicineName ?? '')],
      outstanding: [line.outstandingQuantity],
      quantity: [line.outstandingQuantity, [Validators.min(0), Validators.max(line.outstandingQuantity)]],
      unitUid: [''],
      batchNo: ['', [Validators.required, Validators.maxLength(64)]],
      expiresAt: ['']
    }));
    this.lineForms.set(forms);
    open.forEach((line) => this.loadUnits(line.uid, line.medicineUid));
  }

  private loadUnits(poLineUid: string, medicineUid: string): void {
    this.medicineService.listUnits(medicineUid).subscribe({
      next: (us) => {
        const active = us.filter((u) => u.active);
        this.unitsByLine.update((m) => ({ ...m, [poLineUid]: active }));
        const base = active.find((u) => u.base);
        if (base) {
          const fg = this.lineForms().find((g) => g.controls['poLineUid'].value === poLineUid);
          if (fg) fg.controls['unitUid'].setValue(base.uid);
        }
      }
    });
  }

  unitsFor(poLineUid: string): MedicineUnit[] {
    return this.unitsByLine()[poLineUid] ?? [];
  }

  unitLabel(u: MedicineUnit): string {
    return u.base ? `${u.name} (base)` : `${u.name} (×${u.factorToBase})`;
  }

  private resolveUnitUid(poLineUid: string, selected: string): string | null {
    if (!selected) return null;
    const unit = this.unitsFor(poLineUid).find((u) => u.uid === selected);
    return unit?.base ? null : selected;
  }

  submit(): void {
    if (this.submitting()) return;
    const lines = this.lineForms()
      .map((fg) => {
        const poLineUid = fg.controls['poLineUid'].value as string;
        return {
          poLineUid,
          quantity: Number(fg.controls['quantity'].value) || 0,
          unitUid: this.resolveUnitUid(poLineUid, (fg.controls['unitUid'].value as string) || ''),
          batchNo: ((fg.controls['batchNo'].value as string) || '').trim(),
          expiresAt: ((fg.controls['expiresAt'].value as string) || '').trim() || null
        };
      })
      .filter((l) => l.quantity > 0);
    if (lines.length === 0) {
      this.errorMessage.set('Enter at least one line quantity greater than zero.');
      return;
    }
    const missingBatch = lines.find((l) => !l.batchNo);
    if (missingBatch) {
      this.errorMessage.set('Every received line needs a batch number from the supplier.');
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const po = this.po();
    if (!po) return;
    this.purchaseOrderService.recordReceipt(po.uid, {
      deliveryNote: raw.deliveryNote?.trim() || null,
      notes: raw.notes?.trim() || null,
      lines
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (receipt: GoodsReceipt) => this.activeModal.close(receipt),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not record receipt.')
    });
  }
}
