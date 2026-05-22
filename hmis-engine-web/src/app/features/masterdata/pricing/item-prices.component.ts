import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { PriceFormComponent } from './price-form.component';
import { ServicePriceService } from './service-price.service';
import { SERVICE_KINDS, ServiceKind, ServicePrice } from './service-price.types';

/**
 * Item-scoped price manager: lists every price row for one catalogue item
 * (a clinic, lab test, medicine, …) across payers and currencies, and lets the
 * user add / edit / delete them. Opened as a modal from each catalogue's row
 * actions so pricing lives next to the item instead of in one global matrix.
 */
@Component({
  selector: 'app-item-prices',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './item-prices.component.html'
})
export class ItemPricesComponent implements OnInit {
  @Input({ required: true }) kind!: ServiceKind;
  @Input({ required: true }) serviceUid!: string;
  @Input() serviceLabel = '';

  private readonly priceService = inject(ServicePriceService);
  private readonly modal = inject(NgbModal);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly prices = signal<ServicePrice[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void { this.reload(); }

  get kindLabel(): string { return SERVICE_KINDS.find((k) => k.value === this.kind)?.label ?? this.kind; }
  get kindIcon(): string { return SERVICE_KINDS.find((k) => k.value === this.kind)?.icon ?? 'bi-cash-stack'; }

  payerLabel(p: ServicePrice): string {
    return p.planUid === null ? 'Cash / public' : (p.planName ?? p.planUid);
  }

  reload(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.priceService.search({ kind: this.kind, serviceUid: this.serviceUid, size: 200, sort: 'currency,asc' }).subscribe({
      next: (res) => { this.prices.set(res.content); this.loading.set(false); },
      error: (err) => { this.errorMessage.set(err?.error?.message ?? 'Could not load prices.'); this.loading.set(false); }
    });
  }

  add(): void {
    const r = this.modal.open(PriceFormComponent, { size: 'lg', backdrop: 'static' });
    const inst = r.componentInstance as PriceFormComponent;
    inst.presetKind = this.kind;
    inst.presetServiceUid = this.serviceUid;
    inst.presetServiceLabel = this.serviceLabel;
    r.closed.subscribe(() => this.reload());
  }

  edit(p: ServicePrice): void {
    const r = this.modal.open(PriceFormComponent, { size: 'lg', backdrop: 'static' });
    (r.componentInstance as PriceFormComponent).existing = p;
    r.closed.subscribe(() => this.reload());
  }

  remove(p: ServicePrice): void {
    if (!globalThis.confirm(`Delete the ${p.currency} ${this.payerLabel(p)} price? This cannot be undone.`)) return;
    this.priceService.delete(p.uid).subscribe({
      next: () => this.reload(),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not delete price.')
    });
  }
}
