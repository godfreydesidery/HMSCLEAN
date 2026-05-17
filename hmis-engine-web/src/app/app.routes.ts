import { Routes } from '@angular/router';

import { authMatchGuard, hasPrivilegeGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/iam/login/login.component').then((m) => m.LoginComponent)
  },
  {
    path: 'forbidden',
    loadComponent: () =>
      import('./shared/errors/forbidden.component').then((m) => m.ForbiddenComponent)
  },
  {
    path: '',
    canMatch: [authMatchGuard],
    loadComponent: () =>
      import('./shared/layout/shell.component').then((m) => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/iam/dashboard/dashboard.component').then((m) => m.DashboardComponent)
      },
      {
        path: 'iam',
        loadChildren: () => import('./features/iam/iam.routes').then((m) => m.IAM_ROUTES)
      },
      {
        path: 'masterdata',
        canActivate: [hasPrivilegeGuard('MASTERDATA_MANAGE')],
        loadChildren: () =>
          import('./features/masterdata/masterdata.routes').then((m) => m.MASTERDATA_ROUTES)
      },
      {
        path: 'patients',
        canActivate: [hasPrivilegeGuard('PATIENT_ACCESS')],
        loadChildren: () =>
          import('./features/patient/patient.routes').then((m) => m.PATIENT_ROUTES)
      },
      {
        path: 'encounters',
        canActivate: [hasPrivilegeGuard('ENCOUNTER_ACCESS')],
        loadChildren: () =>
          import('./features/encounter/encounter.routes').then((m) => m.ENCOUNTER_ROUTES)
      },
      {
        path: 'orders',
        canActivate: [hasPrivilegeGuard('ORDERS_ACCESS')],
        loadChildren: () =>
          import('./features/orders/orders.routes').then((m) => m.ORDERS_ROUTES)
      },
      {
        path: 'pharmacy',
        canActivate: [hasPrivilegeGuard('PHARMACY_ACCESS')],
        loadChildren: () =>
          import('./features/pharmacy/pharmacy.routes').then((m) => m.PHARMACY_ROUTES)
      },
      {
        path: 'procurement',
        canActivate: [hasPrivilegeGuard('PROCUREMENT_ACCESS')],
        loadChildren: () =>
          import('./features/procurement/procurement.routes').then((m) => m.PROCUREMENT_ROUTES)
      },
      {
        path: 'billing',
        canActivate: [hasPrivilegeGuard('BILLING_ACCESS')],
        loadChildren: () =>
          import('./features/billing/billing.routes').then((m) => m.BILLING_ROUTES)
      },
      {
        path: 'hr',
        canActivate: [hasPrivilegeGuard('HR_ACCESS')],
        loadChildren: () => import('./features/hr/hr.routes').then((m) => m.HR_ROUTES)
      },
      {
        path: 'reporting',
        canActivate: [hasPrivilegeGuard('REPORTING_ACCESS')],
        loadChildren: () =>
          import('./features/reporting/reporting.routes').then((m) => m.REPORTING_ROUTES)
      }
    ]
  },
  { path: '**', redirectTo: '' }
];
