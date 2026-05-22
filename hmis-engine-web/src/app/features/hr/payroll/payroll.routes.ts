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
    path: 'components',
    loadComponent: () => import('./payroll-component-list.component').then((m) => m.PayrollComponentListComponent)
  },
  {
    path: 'components/new',
    loadComponent: () => import('./payroll-component-form.component').then((m) => m.PayrollComponentFormComponent)
  },
  {
    path: 'components/:uid/edit',
    loadComponent: () => import('./payroll-component-form.component').then((m) => m.PayrollComponentFormComponent)
  },
  {
    path: ':uid',
    loadComponent: () => import('./payroll-detail.component').then((m) => m.PayrollDetailComponent)
  }
];
