export type SupplierInvoiceStatus =
  | 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'PAID' | 'REJECTED' | 'CANCELLED';

export const SUPPLIER_INVOICE_STATUSES: { value: SupplierInvoiceStatus; label: string; badgeClass: string }[] = [
  { value: 'DRAFT',     label: 'Draft',     badgeClass: 'text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle' },
  { value: 'SUBMITTED', label: 'Submitted', badgeClass: 'text-bg-info-subtle text-info-emphasis border border-info-subtle' },
  { value: 'APPROVED',  label: 'Approved',  badgeClass: 'text-bg-primary-subtle text-primary border border-primary-subtle' },
  { value: 'PAID',      label: 'Paid',      badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'REJECTED',  label: 'Rejected',  badgeClass: 'text-bg-danger-subtle text-danger-emphasis border border-danger-subtle' },
  { value: 'CANCELLED', label: 'Cancelled', badgeClass: 'text-bg-light text-secondary border' }
];

export type SupplierPaymentMethod = 'CASH' | 'MOBILE_MONEY' | 'BANK_TRANSFER' | 'CARD' | 'INSURANCE_CLAIM';

export const SUPPLIER_PAYMENT_METHODS: { value: SupplierPaymentMethod; label: string }[] = [
  { value: 'CASH',            label: 'Cash' },
  { value: 'MOBILE_MONEY',    label: 'Mobile money' },
  { value: 'BANK_TRANSFER',   label: 'Bank transfer' },
  { value: 'CARD',            label: 'Card' },
  { value: 'INSURANCE_CLAIM', label: 'Insurance claim' }
];

export interface SupplierInvoiceLine {
  uid: string;
  poLineUid: string;
  medicineUid: string | null;
  medicineCode: string | null;
  medicineName: string | null;
  poOrderedQuantity: number;
  poReceivedQuantity: number;
  poInvoicedQuantity: number;
  invoicedQuantity: number;
  unitCost: number;
  lineAmount: number;
}

export interface SupplierInvoice {
  uid: string;
  supplierUid: string;
  supplierName: string | null;
  orderUid: string;
  orderNo: string | null;
  supplierInvoiceNo: string;
  invoiceDate: string;
  dueDate: string | null;
  currency: string;
  totalAmount: number;
  status: SupplierInvoiceStatus;
  submittedAt: string | null;
  submittedByUsername: string | null;
  approvedAt: string | null;
  approvedByUsername: string | null;
  paidAt: string | null;
  paidByUsername: string | null;
  paymentMethod: SupplierPaymentMethod | null;
  paymentReference: string | null;
  rejectedAt: string | null;
  rejectedByUsername: string | null;
  rejectReason: string | null;
  cancelledAt: string | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
  lines: SupplierInvoiceLine[];
}

export interface SupplierInvoiceSummary {
  uid: string;
  supplierName: string | null;
  orderNo: string | null;
  supplierInvoiceNo: string;
  invoiceDate: string;
  currency: string;
  totalAmount: number;
  status: SupplierInvoiceStatus;
  createdAt: string;
}

export interface CreateInvoiceLineRequest {
  poLineUid: string;
  invoicedQuantity: number;
  unitCost: number;
}

export interface CreateSupplierInvoiceRequest {
  orderUid: string;
  supplierInvoiceNo: string;
  invoiceDate: string;
  dueDate: string | null;
  currency: string | null;
  notes: string | null;
  lines: CreateInvoiceLineRequest[];
}

export interface MarkPaidRequest {
  method: SupplierPaymentMethod;
  reference: string | null;
}

export interface SupplierInvoiceSearchParams {
  query?: string;
  status?: SupplierInvoiceStatus;
  supplierUid?: string;
  orderUid?: string;
  page?: number;
  size?: number;
  sort?: string;
}
