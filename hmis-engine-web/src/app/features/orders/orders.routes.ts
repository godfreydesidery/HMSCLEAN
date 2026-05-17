import { Routes } from '@angular/router';

export const ORDERS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('../placeholder/placeholder.component').then((m) => m.PlaceholderComponent),
    data: { title: 'Orders & Results' }
  }
];
