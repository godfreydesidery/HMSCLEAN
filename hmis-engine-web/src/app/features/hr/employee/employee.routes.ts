import { Routes } from '@angular/router';

export const EMPLOYEE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./employee-list.component').then((m) => m.EmployeeListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./employee-form.component').then((m) => m.EmployeeFormComponent)
  },
  {
    path: ':uid',
    loadComponent: () => import('./employee-detail.component').then((m) => m.EmployeeDetailComponent)
  },
  {
    path: ':uid/edit',
    loadComponent: () => import('./employee-edit.component').then((m) => m.EmployeeEditComponent)
  }
];
