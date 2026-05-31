import { Routes } from '@angular/router';

const PS = './pharmacy-store';

export const TRANSFERS_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'ro' },

  // Requisition Orders (pharmacy → store request)
  { path: 'ro', loadComponent: () => import(`${PS}/ro/ro-list.component`).then((m) => m.RoListComponent), data: { title: 'Requisitions' } },
  { path: 'ro/new', loadComponent: () => import(`${PS}/ro/ro-create.component`).then((m) => m.RoCreateComponent), data: { title: 'New requisition' } },
  { path: 'ro/:uid', loadComponent: () => import(`${PS}/ro/ro-detail.component`).then((m) => m.RoDetailComponent), data: { title: 'Requisition' } },

  // Transfer Orders (store issue against an RO)
  { path: 'to', loadComponent: () => import(`${PS}/to/to-list.component`).then((m) => m.ToListComponent), data: { title: 'Transfer orders' } },
  { path: 'to/new', loadComponent: () => import(`${PS}/to/to-create.component`).then((m) => m.ToCreateComponent), data: { title: 'New transfer order' } },
  { path: 'to/:uid', loadComponent: () => import(`${PS}/to/to-detail.component`).then((m) => m.ToDetailComponent), data: { title: 'Transfer order' } },

  // Receive Notes (pharmacy receive against a TO)
  { path: 'rn', loadComponent: () => import(`${PS}/rn/rn-list.component`).then((m) => m.RnListComponent), data: { title: 'Receive notes' } },
  { path: 'rn/new', loadComponent: () => import(`${PS}/rn/rn-create.component`).then((m) => m.RnCreateComponent), data: { title: 'New receive note' } },
  { path: 'rn/:uid', loadComponent: () => import(`${PS}/rn/rn-detail.component`).then((m) => m.RnDetailComponent), data: { title: 'Receive note' } }
];
