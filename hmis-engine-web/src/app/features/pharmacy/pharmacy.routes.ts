import { Routes } from '@angular/router';

import { hasPrivilegeGuard } from '../../core/auth/auth.guard';

export const PHARMACY_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pharmacy-workspace.component').then((m) => m.PharmacyWorkspaceComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dispense-queue' },
      {
        path: 'select',
        loadComponent: () =>
          import('./select/pharmacy-select.component').then((m) => m.PharmacySelectComponent),
        data: { title: 'Select pharmacy' }
      },
      {
        path: 'dispense-queue',
        loadComponent: () =>
          import('./dispense-worklist/dispense-worklist.component').then((m) => m.DispenseWorklistComponent),
        data: { title: 'Dispensing queue' }
      },
      {
        path: 'dispense-queue/patient/:patientUid',
        loadComponent: () =>
          import('./dispense-worklist/patient-dispense.component').then((m) => m.PatientDispenseComponent),
        data: { title: 'Dispense — patient' }
      },
      {
        path: 'stock',
        loadComponent: () =>
          import('./stock/stock-list.component').then((m) => m.StockListComponent),
        data: { title: 'Pharmacy stock' }
      },
      {
        path: 'sales',
        loadComponent: () =>
          import('./sale/pharmacy-sale-list.component').then((m) => m.PharmacySaleListComponent),
        data: { title: 'Pharmacy sales' }
      },
      {
        path: 'sales/new',
        loadComponent: () =>
          import('./sale/new-sale.component').then((m) => m.NewSaleComponent),
        data: { title: 'New sale' }
      },
      {
        path: 'sales/:uid',
        loadComponent: () =>
          import('./sale/pharmacy-sale-detail.component').then((m) => m.PharmacySaleDetailComponent),
        data: { title: 'Pharmacy sale' }
      },
      {
        // Stock transfers render INSIDE the pharmacy workspace so the toolbar persists.
        // Same TRANSFERS_ROUTES are also nested under the store workspace; internal links
        // are relative, so the same screens resolve under both prefixes. Gate kept at
        // STORE_ACCESS to match the original top-level transfers guard.
        path: 'transfers',
        canActivate: [hasPrivilegeGuard('STORE_ACCESS')],
        loadChildren: () =>
          import('../transfers/transfers.routes').then((m) => m.TRANSFERS_ROUTES)
      }
    ]
  }
];
