import { Routes } from '@angular/router';

export const LAB_BATCH_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./lab-batch-list.component').then((m) => m.LabBatchListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./lab-batch-create.component').then((m) => m.LabBatchCreateComponent)
  },
  {
    path: ':uid',
    loadComponent: () => import('./lab-batch-detail.component').then((m) => m.LabBatchDetailComponent)
  }
];
