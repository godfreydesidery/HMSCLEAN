import { PaymentType } from '../../patient/patient.types';

export type PharmacySaleOrderStatus = 'ACTIVE' | 'COMPLETED' | 'CANCELLED';

export const PHARMACY_SALE_ORDER_STATUSES: { value: PharmacySaleOrderStatus; label: string; badgeClass: string }[] = [
  { value: 'ACTIVE',    label: 'Active',    badgeClass: 'text-bg-primary-subtle text-primary border border-primary-subtle' },
  { value: 'COMPLETED', label: 'Completed', badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'CANCELLED', label: 'Cancelled', badgeClass: 'text-bg-light text-secondary border' }
];

export type PharmacySaleLineStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'HELD'
  | 'VERIFIED'
  | 'APPROVED'
  | 'SOLD'
  | 'REJECTED'
  | 'CANCELLED';

export const PHARMACY_SALE_LINE_STATUSES: { value: PharmacySaleLineStatus; label: string; badgeClass: string }[] = [
  { value: 'PENDING',   label: 'Pending',   badgeClass: 'text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle' },
  { value: 'ACCEPTED',  label: 'Accepted',  badgeClass: 'text-bg-info-subtle text-info-emphasis border border-info-subtle' },
  { value: 'HELD',      label: 'Held',      badgeClass: 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle' },
  { value: 'VERIFIED',  label: 'Verified',  badgeClass: 'text-bg-primary-subtle text-primary border border-primary-subtle' },
  { value: 'APPROVED',  label: 'Approved',  badgeClass: 'text-bg-primary-subtle text-primary border border-primary-subtle' },
  { value: 'SOLD',      label: 'Sold',      badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'REJECTED',  label: 'Rejected',  badgeClass: 'text-bg-danger-subtle text-danger-emphasis border border-danger-subtle' },
  { value: 'CANCELLED', label: 'Cancelled', badgeClass: 'text-bg-light text-secondary border' }
];

export interface PharmacySaleOrderLine {
  uid: string;
  medicineUid: string;
  medicineCode: string | null;
  medicineName: string | null;
  medicineStrength: string | null;
  quantity: number;
  dose: string | null;
  frequency: string | null;
  durationDays: number | null;
  instructions: string | null;
  unitPrice: number;
  lineAmount: number;
  status: PharmacySaleLineStatus;
  acceptedAt: string | null;
  heldAt: string | null;
  verifiedAt: string | null;
  approvedAt: string | null;
  soldAt: string | null;
  rejectedAt: string | null;
  rejectReason: string | null;
  cancelReason: string | null;
  createdAt: string;
}

export interface PharmacySaleOrder {
  uid: string;
  saleNo: string;
  pharmacyUid: string;
  pharmacyName: string | null;
  patientUid: string | null;
  patientNo: string | null;
  customerName: string;
  customerPhone: string | null;
  status: PharmacySaleOrderStatus;
  paymentType: PaymentType;
  insurancePlanUid: string | null;
  insurancePlanName: string | null;
  currency: string;
  subtotal: number;
  totalPaid: number;
  balance: number;
  openedAt: string;
  completedAt: string | null;
  cancelledAt: string | null;
  cancelReason: string | null;
  createdAt: string;
  updatedAt: string;
  lines: PharmacySaleOrderLine[];
}

export interface PharmacySaleOrderSummary {
  uid: string;
  saleNo: string;
  pharmacyName: string | null;
  customerName: string;
  patientUid: string | null;
  status: PharmacySaleOrderStatus;
  paymentType: PaymentType;
  subtotal: number;
  totalPaid: number;
  balance: number;
  currency: string;
  openedAt: string;
  completedAt: string | null;
}

export interface AddLineRequest {
  medicineUid: string;
  quantity: number;
  dose: string | null;
  frequency: string | null;
  durationDays: number | null;
  instructions: string | null;
  unitPrice: number;
}

export interface CreatePharmacySaleOrderRequest {
  pharmacyUid: string;
  customerName: string;
  customerPhone: string | null;
  patientUid: string | null;
  paymentType: PaymentType;
  insurancePlanUid: string | null;
  lines: AddLineRequest[];
}

export interface PharmacySaleOrderSearchParams {
  query?: string;
  status?: PharmacySaleOrderStatus;
  pharmacyUid?: string;
  patientUid?: string;
  page?: number;
  size?: number;
  sort?: string;
}
