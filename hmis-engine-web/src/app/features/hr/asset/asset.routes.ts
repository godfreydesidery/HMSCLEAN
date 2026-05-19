import { Routes } from '@angular/router';

export const ASSET_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./asset-list.component').then((m) => m.AssetListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./asset-form.component').then((m) => m.AssetFormComponent)
  },
  {
    path: ':uid',
    loadComponent: () => import('./asset-detail.component').then((m) => m.AssetDetailComponent)
  },
  {
    path: ':uid/edit',
    loadComponent: () => import('./asset-edit.component').then((m) => m.AssetEditComponent)
  }
];
