import { Routes } from '@angular/router';

export const PAYROLL_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./payroll-list.component').then((m) => m.PayrollListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./payroll-create.component').then((m) => m.PayrollCreateComponent)
  },
  {
    path: ':uid',
    loadComponent: () => import('./payroll-detail.component').then((m) => m.PayrollDetailComponent)
  }
];
