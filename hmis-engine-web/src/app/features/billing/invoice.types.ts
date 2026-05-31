import { PaymentType } from '../patient/patient.types';

export type InvoiceStatus = 'DRAFT' | 'ISSUED' | 'PARTIALLY_PAID' | 'PAID' | 'CANCELLED';

export const INVOICE_STATUSES: { value: InvoiceStatus; label: string; badgeClass: string }[] = [
  { value: 'DRAFT',          label: 'Draft',          badgeClass: 'text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle' },
  { value: 'ISSUED',         label: 'Issued',         badgeClass: 'text-bg-info-subtle text-info-emphasis border border-info-subtle' },
  { value: 'PARTIALLY_PAID', label: 'Partially paid', badgeClass: 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle' },
  { value: 'PAID',           label: 'Paid',           badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'CANCELLED',      label: 'Cancelled',      badgeClass: 'text-bg-light text-secondary border' }
];

export type InvoiceLineKind =
  | 'CONSULTATION' | 'LAB_TEST' | 'PROCEDURE' | 'RADIOLOGY' | 'MEDICINE' | 'WARD'
  | 'REGISTRATION' | 'CONSUMABLE';

/** Discriminator on Invoice — what the invoice was raised for. Phase 36. */
export type InvoiceScope = 'CONSULTATION' | 'ADMISSION' | 'OUTSIDER' | 'REGISTRATION';

export type PaymentMethod = 'CASH' | 'MOBILE_MONEY' | 'BANK_TRANSFER' | 'CARD' | 'INSURANCE_CLAIM' | 'OTHER';

export const PAYMENT_METHODS: { value: PaymentMethod; label: string }[] = [
  { value: 'CASH',            label: 'Cash' },
  { value: 'MOBILE_MONEY',    label: 'Mobile money' },
  { value: 'BANK_TRANSFER',   label: 'Bank transfer' },
  { value: 'CARD',            label: 'Card' },
  { value: 'INSURANCE_CLAIM', label: 'Insurance claim' },
  { value: 'OTHER',           label: 'Other' }
];

export type LineCoverageStatus = 'COVERED' | 'VERIFIED' | 'UNPAID';

export const LINE_COVERAGE_STATUSES: { value: LineCoverageStatus; label: string; badgeClass: string }[] = [
  { value: 'COVERED',  label: 'Insurer-covered', badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'VERIFIED', label: 'Owed (insured)',  badgeClass: 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle' },
  { value: 'UNPAID',   label: 'Cash',            badgeClass: 'text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle' }
];

export interface InvoiceLine {
  uid: string;
  kind: InvoiceLineKind;
  serviceUid: string | null;
  referenceUid: string | null;
  description: string;
  quantity: number;
  unitPrice: number;
  amount: number;
  /** Payer routing: COVERED = insurer pays, VERIFIED = insured-but-owed, UNPAID = cash. */
  coverageStatus: LineCoverageStatus;
  membershipNo: string | null;
  payerPlanUid: string | null;
  principalLineUid: string | null;
}

export interface Payment {
  uid: string;
  paymentNo: string;
  method: PaymentMethod;
  amount: number;
  currency: string;
  reference: string | null;
  note: string | null;
  receivedAt: string;
  createdAt: string;
}

export interface Invoice {
  uid: string;
  invoiceNo: string;
  scope: InvoiceScope;
  consultationUid: string | null;
  admissionUid: string | null;
  patientUid: string;
  patientName: string | null;
  patientNo: string | null;
  paymentType: PaymentType;
  insurancePlanUid: string | null;
  insurancePlanName: string | null;
  currency: string;
  subtotal: number;
  totalPaid: number;
  balance: number;
  status: InvoiceStatus;
  issuedAt: string | null;
  paidAt: string | null;
  cancelledAt: string | null;
  cancelReason: string | null;
  createdAt: string;
  updatedAt: string;
  lines: InvoiceLine[];
  payments: Payment[];
}

export interface InvoiceSummary {
  uid: string;
  invoiceNo: string;
  scope: InvoiceScope;
  consultationUid: string | null;
  admissionUid: string | null;
  patientUid: string;
  patientName: string | null;
  patientNo: string | null;
  status: InvoiceStatus;
  paymentType: PaymentType;
  subtotal: number;
  totalPaid: number;
  balance: number;
  currency: string;
  issuedAt: string | null;
  createdAt: string;
}

export interface RecordPaymentRequest {
  method: PaymentMethod;
  amount: number;
  currency: string;
  reference: string | null;
  note: string | null;
}

export interface InvoiceSearchParams {
  query?: string;
  status?: InvoiceStatus;
  patientUid?: string;
  page?: number;
  size?: number;
  sort?: string;
}
