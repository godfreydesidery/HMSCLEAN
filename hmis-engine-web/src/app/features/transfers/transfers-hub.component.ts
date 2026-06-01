import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

interface HubLink { label: string; path: string; icon: string; hint: string; }

/** Landing page for the stock-transfer documents (store↔pharmacy and pharmacy↔pharmacy). */
@Component({
  selector: 'app-transfers-hub',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="hmis-page-header">
      <h1 class="hmis-page-title">Stock transfers</h1>
    </div>

    <h2 class="h6 hmis-muted mt-2 mb-2">Store ↔ pharmacy</h2>
    <div class="row g-3 mb-4">
      @for (l of storeLinks; track l.path) {
        <div class="col-md-6 col-xl-3">
          <a class="card h-100 text-decoration-none text-reset" [routerLink]="l.path">
            <div class="card-body">
              <div class="d-flex align-items-center gap-2 mb-1">
                <i class="bi {{ l.icon }} fs-4 text-primary"></i>
                <span class="fw-semibold">{{ l.label }}</span>
              </div>
              <div class="hmis-muted small">{{ l.hint }}</div>
            </div>
          </a>
        </div>
      }
    </div>

    <h2 class="h6 hmis-muted mt-2 mb-2">Pharmacy ↔ pharmacy</h2>
    <div class="row g-3">
      @for (l of pharmacyLinks; track l.path) {
        <div class="col-md-6 col-xl-3">
          <a class="card h-100 text-decoration-none text-reset" [routerLink]="l.path">
            <div class="card-body">
              <div class="d-flex align-items-center gap-2 mb-1">
                <i class="bi {{ l.icon }} fs-4 text-primary"></i>
                <span class="fw-semibold">{{ l.label }}</span>
              </div>
              <div class="hmis-muted small">{{ l.hint }}</div>
            </div>
          </a>
        </div>
      }
    </div>
  `
})
export class TransfersHubComponent {
  readonly storeLinks: HubLink[] = [
    { label: 'Requisitions (RO)', path: 'ro', icon: 'bi-clipboard-plus', hint: 'Pharmacy requests stock from a store' },
    { label: 'Transfer orders (TO)', path: 'to', icon: 'bi-box-arrow-right', hint: 'Store issues against a requisition' },
    { label: 'Receive notes (RN)', path: 'rn', icon: 'bi-box-arrow-in-down', hint: 'Pharmacy receives the issued stock' },
    { label: 'Returns', path: 'returns', icon: 'bi-arrow-return-left', hint: 'Pharmacy returns stock to a store' }
  ];
  readonly pharmacyLinks: HubLink[] = [
    { label: 'Requisitions (RO)', path: 'pp/ro', icon: 'bi-clipboard-plus', hint: 'A pharmacy requests stock from another' },
    { label: 'Transfer orders (TO)', path: 'pp/to', icon: 'bi-box-arrow-right', hint: 'Delivering pharmacy issues against a requisition' },
    { label: 'Receive notes (RN)', path: 'pp/rn', icon: 'bi-box-arrow-in-down', hint: 'Requesting pharmacy receives the stock' }
  ];
}
