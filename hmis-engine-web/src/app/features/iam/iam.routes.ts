import { Routes } from '@angular/router';

import { hasPrivilegeGuard } from '../../core/auth/auth.guard';

export const IAM_ROUTES: Routes = [
  {
    path: 'users',
    canActivate: [hasPrivilegeGuard('USER_READ')],
    loadComponent: () => import('./users/users.component').then((m) => m.UsersComponent)
  },
  {
    path: 'roles',
    canActivate: [hasPrivilegeGuard('ROLE_READ')],
    loadComponent: () => import('./roles/roles.component').then((m) => m.RolesComponent)
  }
];
