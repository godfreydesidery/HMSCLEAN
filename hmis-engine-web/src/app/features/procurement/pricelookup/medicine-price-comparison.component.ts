import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { MedicinePriceLookupService } from './medicine-price-lookup.service';
import { SupplierItemPrice } from './medicine-price-lookup.types';

/**
 * Per-medicine supplier price comparison. Lists every supplier's quote for one
 * medicine cheapest-first and highlights the current best (the cheapest
 * currently-valid quote a PO line would lock onto). Read-only — procurement uses
 * it to decide who to put the next LPO with.
 */
@Component({
  selector: 'app-medicine-price-comparison',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="modal-header">
      <h5 class="modal-title d-flex align-items-center gap-2">
        <i class="bi bi-bar-chart-line"></i>
        Compare supplier prices
        @if (medicineLabel) { <span class="hmis-muted small">· {{ medicineLabel }}</span> }
      </h5>
      <button type="button" class="btn-close" (click)="activeModal.dismiss()" aria-label="Close"></button>
    </div>
    <div class="modal-body">
      @if (errorMessage()) {
        <div class="alert alert-danger d-flex align-items-center gap-2"><i class="bi bi-exclamation-circle"></i><span>{{ errorMessage() }}</span></div>
      }

      <div class="d-flex align-items-center gap-2 mb-3">
        <fieldset class="btn-group btn-group-sm border-0 p-0 m-0">
          <legend class="visually-hidden">Scope filter</legend>
          <button type="button" class="btn btn-outline-secondary" [class.active]="!activeOnly()" (click)="setActiveOnly(false)" [disabled]="loading()">All quotes</button>
          <button type="button" class="btn btn-outline-secondary" [class.active]="activeOnly()" (click)="setActiveOnly(true)" [disabled]="loading()">Currently valid</button>
        </fieldset>
        @if (best()) {
          <span class="badge text-bg-success-subtle text-success-emphasis border border-success-subtle ms-auto d-flex align-items-center gap-1">
            <i class="bi bi-award"></i>Best: {{ best()!.unitPrice | number:'1.2-2' }} {{ best()!.currency }} · {{ best()!.supplierName }}
          </span>
        }
      </div>

      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0 hmis-table">
          <thead><tr><th style="width:2.5rem;"></th><th>Supplier</th><th class="text-end">Unit price</th><th>Valid</th><th>Status</th></tr></thead>
          <tbody>
            @if (loading()) {
              <tr><td colspan="5"><div class="hmis-empty"><i class="bi bi-hourglass-split"></i><div class="small">Loading…</div></div></td></tr>
            } @else if (rows().length === 0) {
              <tr><td colspan="5"><div class="hmis-empty"><i class="bi bi-tags"></i><div class="fw-semibold mb-1">No supplier quotes</div><div class="small">{{ activeOnly() ? 'No currently-valid quote for this medicine.' : 'No supplier has quoted this medicine yet.' }}</div></div></td></tr>
            } @else {
              @for (p of rows(); track p.uid; let i = $index) {
                <tr [class.table-success]="isBest(p)" [class.opacity-50]="!p.active">
                  <td class="text-center">
                    @if (isBest(p)) {
                      <i class="bi bi-award-fill text-success" title="Current best"></i>
                    } @else {
                      <span class="hmis-muted small">{{ i + 1 }}</span>
                    }
                  </td>
                  <td>
                    <div class="fw-semibold">{{ p.supplierName ?? '—' }}</div>
                    @if (p.notes) { <div class="hmis-muted small">{{ p.notes }}</div> }
                  </td>
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
                </tr>
              }
            }
          </tbody>
        </table>
      </div>
      @if (!loading() && rows().length > 0) {
        <p class="hmis-muted small mt-2 mb-0">Sorted cheapest first. The current best is the cheapest currently-valid quote a purchase-order line would lock onto.</p>
      }
    </div>
    <div class="modal-footer">
      <button type="button" class="btn btn-light border" (click)="activeModal.close()">Close</button>
    </div>
  `
})
export class MedicinePriceComparisonComponent implements OnInit {
  @Input({ required: true }) medicineUid!: string;
  @Input() medicineLabel: string | null = null;

  private readonly lookup = inject(MedicinePriceLookupService);
  protected readonly activeModal = inject(NgbActiveModal);

  private readonly all = signal<SupplierItemPrice[]>([]);
  private readonly active = signal<SupplierItemPrice[]>([]);
  readonly best = signal<SupplierItemPrice | null>(null);
  readonly activeOnly = signal(false);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  /** Cheapest first; backend `/active` is already sorted, full history is sorted client-side. */
  readonly rows = computed<SupplierItemPrice[]>(() =>
    this.activeOnly()
      ? this.active()
      : [...this.all()].sort((a, b) => a.unitPrice - b.unitPrice)
  );

  ngOnInit(): void {
    if (!this.medicineUid) { this.loading.set(false); this.errorMessage.set('Missing medicine identifier.'); return; }
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    forkJoin({
      all: this.lookup.listAll(this.medicineUid),
      active: this.lookup.listActive(this.medicineUid),
      best: this.lookup.currentBest(this.medicineUid)
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ all, active, best }) => { this.all.set(all); this.active.set(active); this.best.set(best); },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load supplier prices.')
      });
  }

  setActiveOnly(v: boolean): void { this.activeOnly.set(v); }

  isBest(p: SupplierItemPrice): boolean {
    return this.best() != null && this.best()!.uid === p.uid;
  }
}
