import { inject } from '@angular/core';
import { CanActivateChildFn, Router, UrlTree } from '@angular/router';

import { WorkingLocationService } from './working-location.service';

/**
 * Cold-start guard for the pharmacy / store workspaces.
 *
 * A workspace operation screen (dispensing queue, stock, transfers…) is only
 * usable once a working location is chosen — the action toolbar (and its
 * "Change" link to the picker) is hidden until then. Without this guard a fresh
 * session (empty localStorage) lands straight on an operation screen with no way
 * to pick a location. This redirects any unselected child to the {@code select}
 * picker; the {@code select} route itself is always allowed (so no redirect loop).
 */
export function requireWorkingLocationGuard(kind: 'pharmacy' | 'store'): CanActivateChildFn {
  return (childRoute): boolean | UrlTree => {
    const workingLocation = inject(WorkingLocationService);
    const router = inject(Router);

    if (childRoute.routeConfig?.path === 'select') {
      return true;
    }
    const selected =
      kind === 'pharmacy' ? workingLocation.workingPharmacy() : workingLocation.workingStore();
    return selected ? true : router.parseUrl(`/${kind}/select`);
  };
}
