import { Routes } from '@angular/router';

export const PHARMACY_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'stock' },
  {
    path: 'stock',
    loadComponent: () =>
      import('./stock/stock-list.component').then((m) => m.StockListComponent),
    data: { title: 'Pharmacy stock' }
  }
];
