import { Injectable, signal } from '@angular/core';

const STORAGE_KEY = 'hmis.tenant';

/**
 * Resolves the current tenant identifier. Today all deployments resolve to
 * the default tenant, but the surface is in place so a future multi-tenant
 * deployment can populate this from the URL / subdomain / JWT.
 */
@Injectable({ providedIn: 'root' })
export class TenantContext {
  private readonly tenantSignal = signal<string>(localStorage.getItem(STORAGE_KEY) ?? 'default');

  readonly tenant = this.tenantSignal.asReadonly();

  set(tenant: string): void {
    localStorage.setItem(STORAGE_KEY, tenant);
    this.tenantSignal.set(tenant);
  }
}
