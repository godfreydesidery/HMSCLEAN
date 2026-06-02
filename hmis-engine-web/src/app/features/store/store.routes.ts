import { Routes } from '@angular/router';

import { hasPrivilegeGuard } from '../../core/auth/auth.guard';
import { requireWorkingLocationGuard } from '../../core/working-location/working-location.guard';

export const STORE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./store-workspace.component').then((m) => m.StoreWorkspaceComponent),
    // Cold start with no working store → bounce to the picker instead of an
    // operation screen with no toolbar (the 'select' child is exempt).
    canActivateChild: [requireWorkingLocationGuard('store')],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'stock' },
      {
        path: 'select',
        loadComponent: () =>
          import('./select/store-select.component').then((m) => m.StoreSelectComponent),
        data: { title: 'Select store' }
      },
      {
        path: 'stock',
        loadComponent: () =>
          import('./stock/store-stock-list.component').then((m) => m.StoreStockListComponent),
        data: { title: 'Store stock' }
      },
      {
        // Stock transfers render INSIDE the store workspace so the toolbar persists.
        // Same TRANSFERS_ROUTES are also nested under the pharmacy workspace; internal
        // links are relative, so the same screens resolve under both prefixes.
        path: 'transfers',
        canActivate: [hasPrivilegeGuard('STORE_ACCESS')],
        loadChildren: () =>
          import('../transfers/transfers.routes').then((m) => m.TRANSFERS_ROUTES)
      }
    ]
  }
];
