/**
 * Frontend DTOs for the consumable subsystem:
 *
 *   * Consumable           — masterdata row (Phase 28)
 *   * ConsumableSourceKind — STORE | PHARMACY
 *   * ConsumableIssue      — chart row on an admission (Phase 41)
 *   * ConsumableStockBalance / receive + adjust requests — Phase 46
 *
 * Mirrors the backend `*Dto` records.
 */

import { PageResponse } from '../../core/http/page.types';
export type { PageResponse };

// ----- Consumable masterdata -------------------------------------------------

export interface Consumable {
  uid: string;
  code: string;
  name: string;
  unitOfMeasure: string | null;
  description: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ConsumableSearchParams {
  query?: string;
  active?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

export interface CreateConsumableRequest {
  code: string;
  name: string;
  unitOfMeasure: string | null;
  description: string | null;
}

export interface UpdateConsumableRequest {
  name: string;
  unitOfMeasure: string | null;
  description: string | null;
}

// ----- Source discriminator --------------------------------------------------

export type ConsumableSourceKind = 'STORE' | 'PHARMACY';

export const CONSUMABLE_SOURCE_KINDS: readonly { value: ConsumableSourceKind; label: string }[] = [
  { value: 'PHARMACY', label: 'Pharmacy' },
  { value: 'STORE',    label: 'Store' }
];

// ----- Patient consumable chart (Phase 41) ----------------------------------

export interface ConsumableIssue {
  uid: string;
  admissionUid: string;
  consumableUid: string;
  consumableCode: string | null;
  consumableName: string | null;
  sourceKind: ConsumableSourceKind;
  sourceLocationUid: string;
  quantity: number;
  unitCost: string;          // BigDecimal
  lineAmount: string;        // BigDecimal
  issuedByUsername: string;
  issuedAt: string;
  note: string | null;
  createdAt: string;
}

export interface IssueConsumableRequest {
  consumableUid: string;
  sourceKind: ConsumableSourceKind;
  sourceLocationUid: string;
  quantity: number;
  unitCost: string;
  note?: string | null;
}

// ----- Stock side (Phase 46) -------------------------------------------------

export interface ConsumableStockBalanceDto {
  uid: string;
  sourceKind: ConsumableSourceKind;
  sourceUid: string;
  consumableUid: string;
  consumableCode: string | null;
  consumableName: string | null;
  quantity: number;
  updatedAt: string;
}

export interface ReceiveConsumableRequest {
  sourceKind: ConsumableSourceKind;
  sourceLocationUid: string;
  consumableUid: string;
  quantity: number;
  note?: string | null;
}

export interface AdjustConsumableRequest {
  sourceKind: ConsumableSourceKind;
  sourceLocationUid: string;
  consumableUid: string;
  delta: number;
  note?: string | null;
}
