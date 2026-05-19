import { Routes } from '@angular/router';

export const CONSUMABLES_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'stock' },
  {
    path: 'stock',
    loadComponent: () => import('./consumable-stock.component').then((m) => m.ConsumableStockComponent)
  }
];
