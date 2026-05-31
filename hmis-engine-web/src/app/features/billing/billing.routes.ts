import { Routes } from '@angular/router';

export const BILLING_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./invoice-list.component').then((m) => m.InvoiceListComponent)
  },
  {
    path: 'claims',
    loadComponent: () => import('./claims/claim-list.component').then((m) => m.ClaimListComponent)
  },
  {
    path: 'claims/:uid',
    loadComponent: () => import('./claims/claim-detail.component').then((m) => m.ClaimDetailComponent)
  },
  {
    path: ':uid',
    loadComponent: () => import('./invoice-detail.component').then((m) => m.InvoiceDetailComponent)
  }
];
