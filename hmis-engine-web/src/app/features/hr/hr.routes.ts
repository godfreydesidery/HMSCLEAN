import { Routes } from '@angular/router';

export const HR_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'assets' },
  {
    path: 'assets',
    loadChildren: () => import('./asset/asset.routes').then((m) => m.ASSET_ROUTES)
  }
];
