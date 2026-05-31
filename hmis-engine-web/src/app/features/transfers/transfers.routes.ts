import { Routes } from '@angular/router';

const PS = './pharmacy-store';
const PP = './pharmacy-pharmacy';

export const TRANSFERS_ROUTES: Routes = [
  { path: '', pathMatch: 'full', loadComponent: () => import('./transfers-hub.component').then((m) => m.TransfersHubComponent), data: { title: 'Transfers' } },

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
  { path: 'rn/:uid', loadComponent: () => import(`${PS}/rn/rn-detail.component`).then((m) => m.RnDetailComponent), data: { title: 'Receive note' } },

  // Pharmacy → store returns
  { path: 'returns', loadComponent: () => import(`${PS}/returns/return-list.component`).then((m) => m.ReturnListComponent), data: { title: 'Returns' } },
  { path: 'returns/new', loadComponent: () => import(`${PS}/returns/return-create.component`).then((m) => m.ReturnCreateComponent), data: { title: 'New return' } },
  { path: 'returns/:uid', loadComponent: () => import(`${PS}/returns/return-detail.component`).then((m) => m.ReturnDetailComponent), data: { title: 'Return' } },

  // Inter-pharmacy transfer chain (RO → TO → RN)
  { path: 'pp/ro', loadComponent: () => import(`${PP}/ro/pp-ro-list.component`).then((m) => m.PpRoListComponent), data: { title: 'Inter-pharmacy requisitions' } },
  { path: 'pp/ro/new', loadComponent: () => import(`${PP}/ro/pp-ro-create.component`).then((m) => m.PpRoCreateComponent), data: { title: 'New requisition' } },
  { path: 'pp/ro/:uid', loadComponent: () => import(`${PP}/ro/pp-ro-detail.component`).then((m) => m.PpRoDetailComponent), data: { title: 'Requisition' } },
  { path: 'pp/to', loadComponent: () => import(`${PP}/to/pp-to-list.component`).then((m) => m.PpToListComponent), data: { title: 'Inter-pharmacy transfer orders' } },
  { path: 'pp/to/new', loadComponent: () => import(`${PP}/to/pp-to-create.component`).then((m) => m.PpToCreateComponent), data: { title: 'New transfer order' } },
  { path: 'pp/to/:uid', loadComponent: () => import(`${PP}/to/pp-to-detail.component`).then((m) => m.PpToDetailComponent), data: { title: 'Transfer order' } },
  { path: 'pp/rn', loadComponent: () => import(`${PP}/rn/pp-rn-list.component`).then((m) => m.PpRnListComponent), data: { title: 'Inter-pharmacy receive notes' } },
  { path: 'pp/rn/new', loadComponent: () => import(`${PP}/rn/pp-rn-create.component`).then((m) => m.PpRnCreateComponent), data: { title: 'New receive note' } },
  { path: 'pp/rn/:uid', loadComponent: () => import(`${PP}/rn/pp-rn-detail.component`).then((m) => m.PpRnDetailComponent), data: { title: 'Receive note' } }
];
