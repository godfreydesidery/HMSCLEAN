import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NgbDropdownModule, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { DispenseLineModalComponent } from './dispense-line-modal.component';
import { PharmacySaleOrderService } from './pharmacy-sale.service';
import {
  PHARMACY_SALE_LINE_STATUSES, PHARMACY_SALE_ORDER_STATUSES,
  PharmacySaleLineStatus, PharmacySaleOrder, PharmacySaleOrderLine, PharmacySaleOrderStatus
} from './pharmacy-sale.types';

@Component({
  selector: 'app-pharmacy-sale-detail',
  standalone: true,
  imports: [CommonModule, NgbDropdownModule],
  templateUrl: './pharmacy-sale-detail.component.html'
})
export class PharmacySaleDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly saleService = inject(PharmacySaleOrderService);
  private readonly modal = inject(NgbModal);

  readonly orderStatuses = PHARMACY_SALE_ORDER_STATUSES;
  readonly lineStatuses = PHARMACY_SALE_LINE_STATUSES;

  readonly sale = signal<PharmacySaleOrder | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly actionMessage = signal<string | null>(null);

  readonly canCancelSale = computed(() => {
    const s = this.sale();
    return s != null && s.status === 'ACTIVE';
  });

  constructor() {
    const uid = this.route.snapshot.paramMap.get('uid');
    if (!uid) {
      this.errorMessage.set('Missing sale identifier.');
      this.loading.set(false);
      return;
    }
    this.load(uid);
  }

  private load(uid: string): void {
    this.loading.set(true);
    this.saleService.findByUid(uid).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (s) => this.sale.set(s),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load sale.')
    });
  }

  back(): void { void this.router.navigate(['/pharmacy/sales']); }

  acceptLine(line: PharmacySaleOrderLine): void { this.run(this.saleService.acceptLine(this.sale()!.uid, line.uid), 'Line accepted.'); }
  holdLine(line: PharmacySaleOrderLine): void { this.run(this.saleService.holdLine(this.sale()!.uid, line.uid), 'Line on hold.'); }
  verifyLine(line: PharmacySaleOrderLine): void { this.run(this.saleService.verifyLine(this.sale()!.uid, line.uid), 'Line verified.'); }
  approveLine(line: PharmacySaleOrderLine): void { this.run(this.saleService.approveLine(this.sale()!.uid, line.uid), 'Line approved.'); }
  rejectLine(line: PharmacySaleOrderLine): void {
    const reason = globalThis.prompt('Reason for rejecting this line?')?.trim() ?? null;
    if (reason === null) return;
    this.run(this.saleService.rejectLine(this.sale()!.uid, line.uid, reason || null), 'Line rejected.');
  }
  cancelLine(line: PharmacySaleOrderLine): void {
    const reason = globalThis.prompt('Reason for cancelling this line?')?.trim() ?? null;
    if (reason === null) return;
    this.run(this.saleService.cancelLine(this.sale()!.uid, line.uid, reason || null), 'Line cancelled.');
  }

  dispenseLine(line: PharmacySaleOrderLine): void {
    const sale = this.sale(); if (!sale) return;
    // Phase 37 multi-pharmacy override — pick a different pharmacy to pull
    // stock from another location without a formal transfer doc. The modal
    // returns the override uid, or null to keep the default (sale's pharmacy).
    const ref = this.modal.open(DispenseLineModalComponent, { centered: true });
    ref.componentInstance.openedAtPharmacyUid = sale.pharmacyUid;
    ref.componentInstance.openedAtPharmacyName = sale.pharmacyName;
    ref.componentInstance.medicineLabel = line.medicineName || line.medicineUid;
    ref.result.then(
      (salesPharmacyUid: string | null) => {
        this.busy.set(true);
        this.errorMessage.set(null);
        this.saleService.dispenseLine(sale.pharmacyUid, line.uid, salesPharmacyUid)
          .pipe(finalize(() => this.busy.set(false))).subscribe({
            next: () => { this.actionMessage.set('Dispensed — stock decremented.'); this.load(sale.uid); },
            error: (err) => this.errorMessage.set(err?.error?.message ?? 'Dispense failed.')
          });
      },
      () => { /* dismissed — no dispense */ }
    );
  }

  cancelSale(): void {
    const sale = this.sale(); if (!sale) return;
    const reason = globalThis.prompt('Reason for cancelling this sale? Every still-open line will also be cancelled.')?.trim() ?? null;
    if (reason === null) return;
    this.busy.set(true);
    this.saleService.cancel(sale.uid, reason || null).pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (updated) => { this.sale.set(updated); this.actionMessage.set('Sale cancelled.'); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not cancel sale.')
    });
  }

  private run(obs: ReturnType<PharmacySaleOrderService['acceptLine']>, successMsg: string): void {
    this.busy.set(true);
    obs.pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (updated) => { this.sale.set(updated); this.actionMessage.set(successMsg); },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Action failed.')
    });
  }

  orderStatusBadgeClass(s: PharmacySaleOrderStatus): string {
    return 'badge ' + (this.orderStatuses.find((x) => x.value === s)?.badgeClass ?? '');
  }
  orderStatusLabel(s: PharmacySaleOrderStatus): string {
    return this.orderStatuses.find((x) => x.value === s)?.label ?? s;
  }
  lineStatusBadgeClass(s: PharmacySaleLineStatus): string {
    return 'badge ' + (this.lineStatuses.find((x) => x.value === s)?.badgeClass ?? '');
  }
  lineStatusLabel(s: PharmacySaleLineStatus): string {
    return this.lineStatuses.find((x) => x.value === s)?.label ?? s;
  }
}
