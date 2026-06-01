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
    path: 'suppliers/:uid/prices',
    loadComponent: () =>
      import('./supplier/supplier-prices.component').then((m) => m.SupplierPricesComponent),
    data: { title: 'Supplier price list' }
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
  },
  {
    path: 'supplier-invoices',
    loadComponent: () =>
      import('./supplier-invoice/supplier-invoice-list.component').then((m) => m.SupplierInvoiceListComponent),
    data: { title: 'Supplier invoices' }
  },
  {
    path: 'supplier-invoices/:uid',
    loadComponent: () =>
      import('./supplier-invoice/supplier-invoice-detail.component').then((m) => m.SupplierInvoiceDetailComponent),
    data: { title: 'Supplier invoice' }
  }
];
