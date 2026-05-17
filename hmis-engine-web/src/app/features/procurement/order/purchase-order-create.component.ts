import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize, forkJoin } from 'rxjs';

import { StoreService } from '../../masterdata/stores/store.service';
import { Store } from '../../masterdata/stores/store.types';
import { SupplierService } from '../supplier/supplier.service';
import { Supplier } from '../supplier/supplier.types';
import { PurchaseOrderService } from './purchase-order.service';
import { PurchaseOrder } from './purchase-order.types';

@Component({
  selector: 'app-purchase-order-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './purchase-order-create.component.html'
})
export class PurchaseOrderCreateComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly purchaseOrderService = inject(PurchaseOrderService);
  private readonly supplierService = inject(SupplierService);
  private readonly storeService = inject(StoreService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly suppliers = signal<Supplier[]>([]);
  readonly stores = signal<Store[]>([]);
  readonly loadingLookups = signal(true);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    supplierUid: ['', [Validators.required]],
    storeUid: ['', [Validators.required]],
    expectedDeliveryDate: [''],
    notes: ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    forkJoin({
      suppliers: this.supplierService.search({ active: true, size: 200, sort: 'name,asc' }),
      stores: this.storeService.search({ active: true, size: 200, sort: 'name,asc' })
    }).pipe(finalize(() => this.loadingLookups.set(false))).subscribe({
      next: ({ suppliers, stores }) => {
        this.suppliers.set(suppliers.content);
        this.stores.set(stores.content);
      },
      error: () => this.errorMessage.set('Could not load lookups.')
    });
  }

  submit(): void {
    if (this.submitting()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.purchaseOrderService.create({
      supplierUid: raw.supplierUid,
      storeUid: raw.storeUid,
      expectedDeliveryDate: raw.expectedDeliveryDate || null,
      notes: raw.notes?.trim() || null
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (po: PurchaseOrder) => this.activeModal.close(po),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not create purchase order.')
    });
  }
}
