/**
 * Mirrors backend `com.otapp.hmis.engine.hr.asset.*` DTOs (Phase 42).
 */

export type AssetStatus = 'ACTIVE' | 'RETIRED' | 'DISPOSED' | 'LOST';

export interface AssetStatusOption {
  readonly value: AssetStatus;
  readonly label: string;
  readonly badgeClass: string;
}

export const ASSET_STATUSES: readonly AssetStatusOption[] = [
  { value: 'ACTIVE',   label: 'Active',   badgeClass: 'bg-success' },
  { value: 'RETIRED',  label: 'Retired',  badgeClass: 'bg-warning text-dark' },
  { value: 'DISPOSED', label: 'Disposed', badgeClass: 'bg-secondary' },
  { value: 'LOST',     label: 'Lost',     badgeClass: 'bg-danger' }
];

/** Subset of statuses you can transition INTO via the retire endpoint. */
export const RETIRE_TARGETS: readonly AssetStatusOption[] =
  ASSET_STATUSES.filter((s) => s.value !== 'ACTIVE');

export interface Asset {
  uid: string;
  tag: string;
  name: string;
  category: string | null;
  location: string | null;
  description: string | null;
  serialNo: string | null;
  manufacturer: string | null;
  model: string | null;
  acquisitionDate: string | null;
  acquisitionCost: string | null;          // BigDecimal serialised as string
  currency: string | null;
  custodianUsername: string | null;
  status: AssetStatus;
  retiredAt: string | null;
  retiredReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAssetRequest {
  tag: string;
  name: string;
  category?: string | null;
  location?: string | null;
  description?: string | null;
  serialNo?: string | null;
  manufacturer?: string | null;
  model?: string | null;
  acquisitionDate?: string | null;
  acquisitionCost?: string | null;
  currency?: string | null;
  custodianUsername?: string | null;
}

export type UpdateAssetRequest = Omit<CreateAssetRequest, 'tag'>;

export interface RetireAssetRequest {
  target: AssetStatus;       // service validates target != ACTIVE
  date: string;              // ISO yyyy-MM-dd
  reason?: string | null;
}

export interface AssetSearchParams {
  query?: string;
  status?: AssetStatus;
  category?: string;
  location?: string;
  page?: number;
  size?: number;
  sort?: string;
}
