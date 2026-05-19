export type StockMovementKind = 'RECEIPT' | 'DISPENSE' | 'ADJUSTMENT' | 'WASTAGE' | 'TRANSFER_IN' | 'TRANSFER_OUT';

export const STOCK_MOVEMENT_KINDS: { value: StockMovementKind; label: string; badgeClass: string; icon: string }[] = [
  { value: 'RECEIPT',      label: 'Receipt',      badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle', icon: 'bi-box-arrow-in-down' },
  { value: 'DISPENSE',     label: 'Dispense',     badgeClass: 'text-bg-primary-subtle text-primary border border-primary-subtle',           icon: 'bi-box-arrow-up' },
  { value: 'ADJUSTMENT',   label: 'Adjustment',   badgeClass: 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle', icon: 'bi-sliders' },
  { value: 'WASTAGE',      label: 'Wastage',      badgeClass: 'text-bg-danger-subtle text-danger-emphasis border border-danger-subtle',     icon: 'bi-trash' },
  { value: 'TRANSFER_IN',  label: 'Transfer in',  badgeClass: 'text-bg-info-subtle text-info-emphasis border border-info-subtle',           icon: 'bi-arrow-left' },
  { value: 'TRANSFER_OUT', label: 'Transfer out', badgeClass: 'text-bg-info-subtle text-info-emphasis border border-info-subtle',           icon: 'bi-arrow-right' }
];

export interface StockBatch {
  uid: string;
  pharmacyUid: string;
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

export interface StockBalance {
  pharmacyUid: string;
  pharmacyName: string | null;
  medicineUid: string;
  medicineCode: string | null;
  medicineName: string | null;
  medicineStrength: string | null;
  totalQuantity: number;
  batches: number;
  earliestExpiry: string | null;
  batchDetails: StockBatch[];
}

export interface StockMovement {
  uid: string;
  pharmacyUid: string;
  pharmacyName: string | null;
  medicineUid: string;
  medicineCode: string | null;
  medicineName: string | null;
  batchUid: string | null;
  batchNo: string | null;
  kind: StockMovementKind;
  quantity: number;
  balanceAfter: number;
  referenceUid: string | null;
  note: string | null;
  actorUsername: string | null;
  occurredAt: string;
  createdAt: string;
}

export interface ReceiveStockRequest {
  medicineUid: string;
  batchNo: string;
  expiresAt: string | null;
  quantity: number;
  note: string | null;
}

export interface AdjustStockRequest {
  batchUid: string;
  delta: number;
  note: string | null;
}

export type WastageReason = 'EXPIRED' | 'DAMAGED' | 'RECALLED' | 'LOST' | 'OTHER';

export const WASTAGE_REASONS: { value: WastageReason; label: string }[] = [
  { value: 'EXPIRED',  label: 'Expired' },
  { value: 'DAMAGED',  label: 'Damaged' },
  { value: 'RECALLED', label: 'Recalled' },
  { value: 'LOST',     label: 'Lost' },
  { value: 'OTHER',    label: 'Other' }
];

export interface WriteOffStockRequest {
  batchUid: string;
  quantity: number;
  reason: WastageReason;
  unitUid?: string | null;
  note: string | null;
}
