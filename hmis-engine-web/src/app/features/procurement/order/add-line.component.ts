import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { SupplierItemPriceService } from '../supplier/supplier-item-price.service';
import { SupplierItemPrice } from '../supplier/supplier-item-price.types';
import { PurchaseOrderService } from './purchase-order.service';
import { PurchaseOrder, PurchaseOrderLine } from './purchase-order.types';

/**
 * Add / edit a purchase-order line. The item must be one the supplier currently
 * QUOTES (legacy findBySupplierAndItem gate), and the unit price is LOCKED to the
 * supplier's contracted quote — the backend overwrites any sent price, so the UI
 * shows it read-only rather than offering an editable field that is silently
 * discarded.
 */
@Component({
  selector: 'app-po-add-line',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './add-line.component.html'
})
export class AddLineComponent implements OnInit {
  @Input({ required: true }) orderUid!: string;
  /** The order's supplier — drives the quoted-items list (required for new lines). */
  @Input() supplierUid: string | null = null;
  @Input() existing: PurchaseOrderLine | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly purchaseOrderService = inject(PurchaseOrderService);
  private readonly priceService = inject(SupplierItemPriceService);
  protected readonly activeModal = inject(NgbActiveModal);

  /** The supplier's currently-valid quotes — the only items that can be ordered. */
  readonly quotes = signal<SupplierItemPrice[]>([]);
  readonly loadingQuotes = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    medicineUid: ['', [Validators.required]],
    orderedQuantity: [1, [Validators.required, Validators.min(1)]]
  });

  /** The locked contracted price for the chosen item (from the quote, or the existing line on edit). */
  readonly selectedPrice = signal<{ unitPrice: number; currency: string } | null>(null);

  get isEdit(): boolean { return this.existing != null; }
  get title(): string { return this.isEdit ? 'Edit line' : 'Add line'; }
  readonly noQuotes = computed(() => !this.isEdit && !this.loadingQuotes() && this.quotes().length === 0);

  ngOnInit(): void {
    if (this.existing) {
      this.form.patchValue({
        medicineUid: this.existing.medicineUid,
        orderedQuantity: this.existing.orderedQuantity
      });
      this.form.controls.medicineUid.disable();
      this.selectedPrice.set({ unitPrice: this.existing.unitCost, currency: this.existing.currency });
    } else if (this.supplierUid) {
      this.loadingQuotes.set(true);
      this.priceService.list(this.supplierUid)
        .pipe(finalize(() => this.loadingQuotes.set(false)))
        .subscribe({
          next: (rows) => this.quotes.set(rows.filter((q) => q.currentlyValid)),
          error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load supplier prices.')
        });
    }
  }

  onMedicineChange(uid: string): void {
    const q = this.quotes().find((x) => x.medicineUid === uid);
    this.selectedPrice.set(q ? { unitPrice: q.unitPrice, currency: q.currency } : null);
  }

  submit(): void {
    if (this.submitting()) return;
    const price = this.selectedPrice();
    if (this.form.invalid || !price) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const obs = this.isEdit
      ? this.purchaseOrderService.updateLine(this.orderUid, this.existing!.uid, {
          orderedQuantity: raw.orderedQuantity,
          unitCost: price.unitPrice,
          currency: price.currency
        })
      : this.purchaseOrderService.addLine(this.orderUid, {
          medicineUid: raw.medicineUid,
          orderedQuantity: raw.orderedQuantity,
          unitCost: price.unitPrice,
          currency: price.currency
        });
    obs.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (po: PurchaseOrder) => this.activeModal.close(po),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save line.')
    });
  }
}
