import { Routes } from '@angular/router';

export const BILLING_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./invoice-list.component').then((m) => m.InvoiceListComponent)
  },
  {
    path: ':uid',
    loadComponent: () => import('./invoice-detail.component').then((m) => m.InvoiceDetailComponent)
  }
];
