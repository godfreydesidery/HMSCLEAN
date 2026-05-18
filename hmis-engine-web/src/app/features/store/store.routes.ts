import { Routes } from '@angular/router';

export const STORE_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'stock' },
  {
    path: 'stock',
    loadComponent: () =>
      import('./stock/store-stock-list.component').then((m) => m.StoreStockListComponent),
    data: { title: 'Store stock' }
  }
];
