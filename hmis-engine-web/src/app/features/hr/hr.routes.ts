import { Routes } from '@angular/router';

export const HR_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'employees' },
  {
    path: 'employees',
    loadChildren: () => import('./employee/employee.routes').then((m) => m.EMPLOYEE_ROUTES)
  },
  {
    path: 'assets',
    loadChildren: () => import('./asset/asset.routes').then((m) => m.ASSET_ROUTES)
  },
  {
    path: 'payroll',
    loadChildren: () => import('./payroll/payroll.routes').then((m) => m.PAYROLL_ROUTES)
  }
];
