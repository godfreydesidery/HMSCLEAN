export type StoreStockMovementKind = 'RECEIPT' | 'ISSUE' | 'DIRECT_ISSUE' | 'ADJUSTMENT' | 'WASTAGE' | 'RETURN';

export const STORE_STOCK_MOVEMENT_KINDS: { value: StoreStockMovementKind; label: string; badgeClass: string; icon: string }[] = [
  { value: 'RECEIPT',      label: 'Receipt',      badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle', icon: 'bi-box-arrow-in-down' },
  { value: 'ISSUE',        label: 'Issue',        badgeClass: 'text-bg-info-subtle text-info-emphasis border border-info-subtle',           icon: 'bi-arrow-right' },
  { value: 'DIRECT_ISSUE', label: 'Direct issue', badgeClass: 'text-bg-primary-subtle text-primary border border-primary-subtle',           icon: 'bi-box-arrow-up' },
  { value: 'ADJUSTMENT',   label: 'Adjustment',   badgeClass: 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle', icon: 'bi-sliders' },
  { value: 'WASTAGE',      label: 'Wastage',      badgeClass: 'text-bg-danger-subtle text-danger-emphasis border border-danger-subtle',     icon: 'bi-trash' },
  { value: 'RETURN',       label: 'Return',       badgeClass: 'text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle', icon: 'bi-arrow-counterclockwise' }
];

export interface StoreStockBatch {
  uid: string;
  storeUid: string;
  medicineUid: string;
  medicineCode: string | null;
  medicineName: string | null;
  medicineStrength: string | null;
  batchNo: string;
  expiresAt: string | null;
  expired: boolean;
  quantity: number;
  receivedAt: string;
}

export interface StoreStockBalance {
  storeUid: string;
  storeName: string | null;
  medicineUid: string;
  medicineCode: string | null;
  medicineName: string | null;
  medicineStrength: string | null;
  totalQuantity: number;
  batches: number;
  earliestExpiry: string | null;
  batchDetails: StoreStockBatch[];
}

export interface StoreStockMovement {
  uid: string;
  storeUid: string;
  storeName: string | null;
  medicineUid: string;
  medicineCode: string | null;
  medicineName: string | null;
  batchUid: string | null;
  batchNo: string | null;
  kind: StoreStockMovementKind;
  quantity: number;
  balanceAfter: number;
  referenceUid: string | null;
  note: string | null;
  actorUsername: string | null;
  occurredAt: string;
  createdAt: string;
}

export interface ReceiveStoreStockRequest {
  medicineUid: string;
  batchNo: string;
  expiresAt: string | null;
  quantity: number;
  note: string | null;
}

export interface AdjustStoreStockRequest {
  batchUid: string;
  delta: number;
  note: string | null;
}
