import { Routes } from '@angular/router';

export const PROCUREMENT_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'orders' },
  {
    path: 'suppliers',
    loadComponent: () =>
      import('./supplier/supplier-list.component').then((m) => m.SupplierListComponent),
    data: { title: 'Suppliers' }
  },
  {
    path: 'orders',
    loadComponent: () =>
      import('./order/purchase-order-list.component').then((m) => m.PurchaseOrderListComponent),
    data: { title: 'Purchase orders' }
  },
  {
    path: 'orders/:uid',
    loadComponent: () =>
      import('./order/purchase-order-detail.component').then((m) => m.PurchaseOrderDetailComponent),
    data: { title: 'Purchase order' }
  }
];
