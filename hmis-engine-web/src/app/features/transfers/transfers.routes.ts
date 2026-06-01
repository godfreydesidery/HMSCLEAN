import { Routes } from '@angular/router';

// NOTE: lazy-import specifiers MUST be static string literals — esbuild / the Vite
// dev server cannot statically analyse a template-literal specifier (e.g.
// `import(`${PS}/ro/...`)`), so those silently fail at runtime with
// "Failed to fetch dynamically imported module". Keep every path inline.
export const TRANSFERS_ROUTES: Routes = [
  { path: '', pathMatch: 'full', loadComponent: () => import('./transfers-hub.component').then((m) => m.TransfersHubComponent), data: { title: 'Transfers' } },

  // Requisition Orders (pharmacy → store request)
  { path: 'ro', loadComponent: () => import('./pharmacy-store/ro/ro-list.component').then((m) => m.RoListComponent), data: { title: 'Requisitions' } },
  { path: 'ro/new', loadComponent: () => import('./pharmacy-store/ro/ro-create.component').then((m) => m.RoCreateComponent), data: { title: 'New requisition' } },
  { path: 'ro/:uid', loadComponent: () => import('./pharmacy-store/ro/ro-detail.component').then((m) => m.RoDetailComponent), data: { title: 'Requisition' } },

  // Transfer Orders (store issue against an RO)
  { path: 'to', loadComponent: () => import('./pharmacy-store/to/to-list.component').then((m) => m.ToListComponent), data: { title: 'Transfer orders' } },
  { path: 'to/new', loadComponent: () => import('./pharmacy-store/to/to-create.component').then((m) => m.ToCreateComponent), data: { title: 'New transfer order' } },
  { path: 'to/:uid', loadComponent: () => import('./pharmacy-store/to/to-detail.component').then((m) => m.ToDetailComponent), data: { title: 'Transfer order' } },

  // Receive Notes (pharmacy receive against a TO)
  { path: 'rn', loadComponent: () => import('./pharmacy-store/rn/rn-list.component').then((m) => m.RnListComponent), data: { title: 'Receive notes' } },
  { path: 'rn/new', loadComponent: () => import('./pharmacy-store/rn/rn-create.component').then((m) => m.RnCreateComponent), data: { title: 'New receive note' } },
  { path: 'rn/:uid', loadComponent: () => import('./pharmacy-store/rn/rn-detail.component').then((m) => m.RnDetailComponent), data: { title: 'Receive note' } },

  // Pharmacy → store returns
  { path: 'returns', loadComponent: () => import('./pharmacy-store/returns/return-list.component').then((m) => m.ReturnListComponent), data: { title: 'Returns' } },
  { path: 'returns/new', loadComponent: () => import('./pharmacy-store/returns/return-create.component').then((m) => m.ReturnCreateComponent), data: { title: 'New return' } },
  { path: 'returns/:uid', loadComponent: () => import('./pharmacy-store/returns/return-detail.component').then((m) => m.ReturnDetailComponent), data: { title: 'Return' } },

  // Inter-pharmacy transfer chain (RO → TO → RN)
  { path: 'pp/ro', loadComponent: () => import('./pharmacy-pharmacy/ro/pp-ro-list.component').then((m) => m.PpRoListComponent), data: { title: 'Inter-pharmacy requisitions' } },
  { path: 'pp/ro/new', loadComponent: () => import('./pharmacy-pharmacy/ro/pp-ro-create.component').then((m) => m.PpRoCreateComponent), data: { title: 'New requisition' } },
  { path: 'pp/ro/:uid', loadComponent: () => import('./pharmacy-pharmacy/ro/pp-ro-detail.component').then((m) => m.PpRoDetailComponent), data: { title: 'Requisition' } },
  { path: 'pp/to', loadComponent: () => import('./pharmacy-pharmacy/to/pp-to-list.component').then((m) => m.PpToListComponent), data: { title: 'Inter-pharmacy transfer orders' } },
  { path: 'pp/to/new', loadComponent: () => import('./pharmacy-pharmacy/to/pp-to-create.component').then((m) => m.PpToCreateComponent), data: { title: 'New transfer order' } },
  { path: 'pp/to/:uid', loadComponent: () => import('./pharmacy-pharmacy/to/pp-to-detail.component').then((m) => m.PpToDetailComponent), data: { title: 'Transfer order' } },
  { path: 'pp/rn', loadComponent: () => import('./pharmacy-pharmacy/rn/pp-rn-list.component').then((m) => m.PpRnListComponent), data: { title: 'Inter-pharmacy receive notes' } },
  { path: 'pp/rn/new', loadComponent: () => import('./pharmacy-pharmacy/rn/pp-rn-create.component').then((m) => m.PpRnCreateComponent), data: { title: 'New receive note' } },
  { path: 'pp/rn/:uid', loadComponent: () => import('./pharmacy-pharmacy/rn/pp-rn-detail.component').then((m) => m.PpRnDetailComponent), data: { title: 'Receive note' } }
];
