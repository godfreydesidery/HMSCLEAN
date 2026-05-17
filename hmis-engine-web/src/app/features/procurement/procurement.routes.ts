import { Routes } from '@angular/router';

export const PROCUREMENT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('../placeholder/placeholder.component').then((m) => m.PlaceholderComponent),
    data: { title: 'Procurement' }
  }
];
