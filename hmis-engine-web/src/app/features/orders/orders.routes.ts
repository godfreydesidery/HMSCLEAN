import { Routes } from '@angular/router';

export const ORDERS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./order-worklist.component').then((m) => m.OrderWorklistComponent)
  }
];
