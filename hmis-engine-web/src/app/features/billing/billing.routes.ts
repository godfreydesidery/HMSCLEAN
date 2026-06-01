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
    path: 'cashier',
    loadComponent: () => import('./cashier-pay.component').then((m) => m.CashierPayComponent)
  },
  {
    path: 'cashier-shift',
    loadComponent: () => import('./my-shift.component').then((m) => m.MyShiftComponent)
  },
  {
    path: 'cashier-shifts',
    loadComponent: () => import('./cashier-shift-list.component').then((m) => m.CashierShiftListComponent)
  },
  {
    path: ':uid',
    loadComponent: () => import('./invoice-detail.component').then((m) => m.InvoiceDetailComponent)
  }
];
