import { Routes } from '@angular/router';

export const PHARMACY_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'stock' },
  {
    path: 'stock',
    loadComponent: () =>
      import('./stock/stock-list.component').then((m) => m.StockListComponent),
    data: { title: 'Pharmacy stock' }
  },
  {
    path: 'sales',
    loadComponent: () =>
      import('./sale/pharmacy-sale-list.component').then((m) => m.PharmacySaleListComponent),
    data: { title: 'Pharmacy sales' }
  },
  {
    path: 'sales/new',
    loadComponent: () =>
      import('./sale/new-sale.component').then((m) => m.NewSaleComponent),
    data: { title: 'New sale' }
  },
  {
    path: 'sales/:uid',
    loadComponent: () =>
      import('./sale/pharmacy-sale-detail.component').then((m) => m.PharmacySaleDetailComponent),
    data: { title: 'Pharmacy sale' }
  }
];
