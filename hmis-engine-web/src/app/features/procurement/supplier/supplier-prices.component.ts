import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { SupplierItemPriceService } from './supplier-item-price.service';
import { SupplierItemPrice } from './supplier-item-price.types';
import { SupplierPriceFormComponent } from './supplier-price-form.component';

/** Manage the item prices a supplier quotes (the source for the PO add-line gate). */
@Component({
  selector: 'app-supplier-prices',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="d-flex align-items-center gap-2 mb-3">
      <a class="btn btn-light border btn-sm" routerLink="/procurement/suppliers"><i class="bi bi-arrow-left"></i></a>
      <h1 class="h5 mb-0">Supplier price list</h1>
      <button type="button" class="btn btn-primary btn-sm ms-auto d-flex align-items-center gap-1" (click)="openForm(null)">
        <i class="bi bi-plus-lg"></i> Add quote
      </button>
    </div>

    @if (errorMessage()) {
      <div class="alert alert-danger d-flex align-items-center gap-2"><i class="bi bi-exclamation-circle"></i><span>{{ errorMessage() }}</span></div>
    }

    <div class="card">
      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0 hmis-table">
          <thead><tr><th>Medicine</th><th class="text-end">Unit price</th><th>Valid</th><th>Status</th><th class="text-end"></th></tr></thead>
          <tbody>
            @if (loading()) {
              <tr><td colspan="5"><div class="hmis-empty"><i class="bi bi-hourglass-split"></i><div class="small">Loading…</div></div></td></tr>
            } @else if (prices().length === 0) {
              <tr><td colspan="5"><div class="hmis-empty"><i class="bi bi-tag"></i><div class="fw-semibold mb-1">No quotes yet</div><div class="small">Add the prices this supplier quotes so they can be ordered.</div></div></td></tr>
            } @else {
              @for (p of prices(); track p.uid) {
                <tr [class.opacity-50]="!p.active">
                  <td class="fw-semibold">{{ p.medicineCode }} — {{ p.medicineName }}@if (p.medicineStrength) { <span class="hmis-muted"> · {{ p.medicineStrength }}</span> }</td>
                  <td class="text-end fw-semibold">{{ p.unitPrice | number:'1.2-2' }} {{ p.currency }}</td>
                  <td class="hmis-muted small">{{ p.validFrom | date:'mediumDate' }} → {{ p.validTo ? (p.validTo | date:'mediumDate') : '∞' }}</td>
                  <td>
                    @if (p.currentlyValid) {
                      <span class="badge text-bg-success-subtle text-success-emphasis border border-success-subtle">Current</span>
                    } @else if (p.active) {
                      <span class="badge text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle">Out of window</span>
                    } @else {
                      <span class="badge text-bg-light border">Inactive</span>
                    }
                  </td>
                  <td class="text-end">
                    <div class="btn-group btn-group-sm">
                      <button type="button" class="btn btn-light border" (click)="openForm(p)" title="Edit"><i class="bi bi-pencil"></i></button>
                      <button type="button" class="btn btn-light border" (click)="toggleActive(p)" [title]="p.active ? 'Deactivate' : 'Activate'">
                        <i class="bi" [class.bi-toggle-on]="p.active" [class.bi-toggle-off]="!p.active"></i>
                      </button>
                      <button type="button" class="btn btn-light border text-danger" (click)="remove(p)" title="Delete"><i class="bi bi-trash"></i></button>
                    </div>
                  </td>
                </tr>
              }
            }
          </tbody>
        </table>
      </div>
    </div>
  `
})
export class SupplierPricesComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly priceService = inject(SupplierItemPriceService);
  private readonly modal = inject(NgbModal);

  readonly supplierUid = this.route.snapshot.paramMap.get('uid') ?? '';
  readonly prices = signal<SupplierItemPrice[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  constructor() {
    if (!this.supplierUid) { this.loading.set(false); this.errorMessage.set('Missing supplier identifier.'); return; }
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.priceService.list(this.supplierUid)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (rows) => this.prices.set(rows),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load the price list.')
      });
  }

  openForm(existing: SupplierItemPrice | null): void {
    const ref = this.modal.open(SupplierPriceFormComponent, { size: 'lg', backdrop: 'static' });
    const inst = ref.componentInstance as SupplierPriceFormComponent;
    inst.supplierUid = this.supplierUid;
    inst.existing = existing;
    ref.closed.subscribe((saved?: SupplierItemPrice) => { if (saved) this.load(); });
  }

  toggleActive(p: SupplierItemPrice): void {
    this.priceService.setActive(this.supplierUid, p.uid, !p.active).subscribe({
      next: (updated) => this.prices.update((rows) => rows.map((r) => r.uid === updated.uid ? updated : r)),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not update the quote.')
    });
  }

  remove(p: SupplierItemPrice): void {
    if (!globalThis.confirm(`Delete the quote for "${p.medicineName ?? p.medicineUid}"?`)) return;
    this.priceService.delete(this.supplierUid, p.uid).subscribe({
      next: () => this.prices.update((rows) => rows.filter((r) => r.uid !== p.uid)),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not delete the quote.')
    });
  }
}
