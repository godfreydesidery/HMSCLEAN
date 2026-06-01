export type PurchaseOrderStatus =
  | 'DRAFT' | 'VERIFIED' | 'APPROVED' | 'ORDERED'
  | 'PARTIALLY_RECEIVED' | 'RECEIVED' | 'REJECTED' | 'CANCELLED';

export const PURCHASE_ORDER_STATUSES: { value: PurchaseOrderStatus; label: string; badgeClass: string }[] = [
  { value: 'DRAFT',              label: 'Draft',              badgeClass: 'text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle' },
  { value: 'VERIFIED',           label: 'Verified',           badgeClass: 'text-bg-info-subtle text-info-emphasis border border-info-subtle' },
  { value: 'APPROVED',           label: 'Approved',           badgeClass: 'text-bg-primary-subtle text-primary border border-primary-subtle' },
  { value: 'ORDERED',            label: 'Ordered',            badgeClass: 'text-bg-info-subtle text-info-emphasis border border-info-subtle' },
  { value: 'PARTIALLY_RECEIVED', label: 'Partially received', badgeClass: 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle' },
  { value: 'RECEIVED',           label: 'Received',           badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'REJECTED',           label: 'Rejected',           badgeClass: 'text-bg-danger-subtle text-danger-emphasis border border-danger-subtle' },
  { value: 'CANCELLED',          label: 'Cancelled',          badgeClass: 'text-bg-light text-secondary border' }
];

export type GoodsReceiptStatus = 'PENDING' | 'VERIFIED' | 'APPROVED' | 'REJECTED';

export const GOODS_RECEIPT_STATUSES: { value: GoodsReceiptStatus; label: string; badgeClass: string }[] = [
  { value: 'PENDING',  label: 'Pending',  badgeClass: 'text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle' },
  { value: 'VERIFIED', label: 'Verified', badgeClass: 'text-bg-info-subtle text-info-emphasis border border-info-subtle' },
  { value: 'APPROVED', label: 'Approved', badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'REJECTED', label: 'Rejected', badgeClass: 'text-bg-danger-subtle text-danger-emphasis border border-danger-subtle' }
];

export interface PurchaseOrderLine {
  uid: string;
  medicineUid: string;
  medicineCode: string | null;
  medicineName: string | null;
  medicineStrength: string | null;
  orderedQuantity: number;
  receivedQuantity: number;
  outstandingQuantity: number;
  unitCost: number;
  currency: string;
  lineAmount: number;
}

export interface PurchaseOrder {
  uid: string;
  orderNo: string;
  supplierUid: string;
  supplierName: string | null;
  storeUid: string;
  storeName: string | null;
  status: PurchaseOrderStatus;
  expectedDeliveryDate: string | null;
  notes: string | null;
  verifiedAt: string | null;
  approvedAt: string | null;
  orderedAt: string | null;
  receivedAt: string | null;
  rejectedAt: string | null;
  rejectReason: string | null;
  cancelledAt: string | null;
  cancelReason: string | null;
  currency: string;
  subtotal: number;
  createdAt: string;
  updatedAt: string;
  lines: PurchaseOrderLine[];
}

export interface PurchaseOrderSummary {
  uid: string;
  orderNo: string;
  supplierName: string | null;
  storeName: string | null;
  status: PurchaseOrderStatus;
  expectedDeliveryDate: string | null;
  subtotal: number;
  currency: string;
  createdAt: string;
}

export interface CreatePurchaseOrderRequest {
  supplierUid: string;
  storeUid: string;
  expectedDeliveryDate: string | null;
  notes: string | null;
}

export interface AddLineRequest {
  medicineUid: string;
  orderedQuantity: number;
  unitCost: number;
  currency: string | null;
}

export interface UpdateLineRequest {
  orderedQuantity: number;
  unitCost: number;
  currency: string | null;
}

export interface PurchaseOrderSearchParams {
  query?: string;
  status?: PurchaseOrderStatus;
  supplierUid?: string;
  storeUid?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface ReceiveLineRequest {
  poLineUid: string;
  quantity: number;
  unitUid?: string | null;
  batchNo: string;
  expiresAt: string | null;
}

export interface RecordReceiptRequest {
  deliveryNote: string | null;
  notes: string | null;
  lines: ReceiveLineRequest[];
}

export interface GoodsReceiptLine {
  uid: string;
  poLineUid: string;
  medicineUid: string;
  medicineCode: string | null;
  medicineName: string | null;
  quantity: number;
  batchNo: string;
  expiresAt: string | null;
}

export interface GoodsReceipt {
  uid: string;
  receiptNo: string;
  orderUid: string;
  orderNo: string | null;
  storeUid: string;
  storeName: string | null;
  receivedByUsername: string | null;
  deliveryNote: string | null;
  notes: string | null;
  status: GoodsReceiptStatus;
  receivedAt: string;
  verifiedAt: string | null;
  verifiedByUsername: string | null;
  approvedAt: string | null;
  approvedByUsername: string | null;
  rejectedAt: string | null;
  rejectedByUsername: string | null;
  rejectReason: string | null;
  createdAt: string;
  lines: GoodsReceiptLine[];
}
