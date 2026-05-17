import { Routes } from '@angular/router';

export const PHARMACY_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('../placeholder/placeholder.component').then((m) => m.PlaceholderComponent),
    data: { title: 'Pharmacy & Inventory' }
  }
];
