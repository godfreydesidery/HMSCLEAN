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

/**
 * The cashier service tills (legacy segmented payment screens): Registration |
 * Consultation | Lab | Radiology | Procedure | Medication | Ward | Consumable.
 * Used to segment a patient's payable lines into per-service queues.
 */
export const INVOICE_LINE_KINDS: { value: InvoiceLineKind; label: string; icon: string }[] = [
  { value: 'REGISTRATION', label: 'Registration', icon: 'bi-person-plus' },
  { value: 'CONSULTATION', label: 'Consultation', icon: 'bi-clipboard2-pulse' },
  { value: 'LAB_TEST',     label: 'Lab',          icon: 'bi-droplet' },
  { value: 'RADIOLOGY',    label: 'Radiology',    icon: 'bi-radioactive' },
  { value: 'PROCEDURE',    label: 'Procedure',    icon: 'bi-scissors' },
  { value: 'MEDICINE',     label: 'Medication',   icon: 'bi-capsule' },
  { value: 'WARD',         label: 'Ward',         icon: 'bi-hospital' },
  { value: 'CONSUMABLE',   label: 'Consumable',   icon: 'bi-bandaid' }
];

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
  /** Cash applied to this line; paidAmount === amount means fully paid. */
  paidAmount: number;
  /** Cash still owed on this line (amount − paidAmount); 0 for insurer-covered. */
  outstanding: number;
  /** Negotiable price band (null = unbounded on that side). */
  minUnitPrice: number | null;
  maxUnitPrice: number | null;
  /** True while the unit price may still be renegotiated (DRAFT/ISSUED + nothing settled). */
  priceOverridable: boolean;
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

/** Admission discharge bill-clearance gate read (AdmissionBillingSummaryDto). */
export interface AdmissionBillingSummary {
  id: number;
  invoiceUid: string;
  invoiceNo: string;
  status: InvoiceStatus;
  subtotal: number;
  totalPaid: number;
  totalCredited: number;
  balance: number;
  cleared: boolean;
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

// ----- Credit notes (write-downs) -------------------------------------------

export type CreditNoteReason =
  | 'HARDSHIP' | 'GOODWILL' | 'ERROR_CORRECTION' | 'SERVICE_NOT_RENDERED' | 'ROUNDING' | 'OTHER';

export const CREDIT_NOTE_REASONS: { value: CreditNoteReason; label: string }[] = [
  { value: 'HARDSHIP',             label: 'Hardship' },
  { value: 'GOODWILL',            label: 'Goodwill' },
  { value: 'ERROR_CORRECTION',    label: 'Error correction' },
  { value: 'SERVICE_NOT_RENDERED', label: 'Service not rendered' },
  { value: 'ROUNDING',            label: 'Rounding' },
  { value: 'OTHER',               label: 'Other' }
];

export interface CreditNote {
  uid: string;
  noteNo: string;
  invoiceUid: string;
  amount: number;
  currency: string;
  reason: CreditNoteReason;
  description: string | null;
  issuedByUsername: string;
  issuedAt: string;
  createdAt: string;
}

export interface CreateCreditNoteRequest {
  amount: number;
  reason: CreditNoteReason;
  description: string | null;
}

// ----- Refunds (return cash already paid) -----------------------------------

export type RefundReason =
  | 'OVERPAYMENT' | 'SERVICE_NOT_RENDERED' | 'DOUBLE_PAYMENT' | 'CANCELLATION' | 'OTHER';

export const REFUND_REASONS: { value: RefundReason; label: string }[] = [
  { value: 'OVERPAYMENT',          label: 'Overpayment' },
  { value: 'SERVICE_NOT_RENDERED', label: 'Service not rendered' },
  { value: 'DOUBLE_PAYMENT',       label: 'Double payment' },
  { value: 'CANCELLATION',         label: 'Cancellation' },
  { value: 'OTHER',                label: 'Other' }
];

export interface Refund {
  uid: string;
  refundNo: string;
  invoiceUid: string;
  amount: number;
  currency: string;
  method: PaymentMethod;
  reason: RefundReason;
  description: string | null;
  reference: string | null;
  refundedByUsername: string;
  refundedAt: string;
  createdAt: string;
}

export interface CreateRefundRequest {
  amount: number;
  method: PaymentMethod;
  reason: RefundReason;
  description: string | null;
  reference: string | null;
}

export interface OverrideLinePriceRequest {
  unitPrice: number;
}

// ----- Cashier line-level payment (legacy "check to pay") --------------------

/**
 * One cash-payable line in a patient's cashier queue, flattened across the
 * patient's open invoices (legacy UNPAID PatientBill). The cashier ticks these
 * and collects `outstanding` per ticked line.
 */
export interface PayableLine {
  invoiceUid: string;
  invoiceNo: string;
  scope: InvoiceScope;
  lineUid: string;
  kind: InvoiceLineKind;
  description: string;
  quantity: number;
  unitPrice: number;
  amount: number;
  paidAmount: number;
  outstanding: number;
  coverageStatus: LineCoverageStatus;
  currency: string;
}

/** Collect cash for the ticked lines (legacy confirm_bills_payment). */
export interface PayLinesRequest {
  method: PaymentMethod;
  currency: string;
  reference: string | null;
  note: string | null;
  lineUids: string[];
}

export interface PayLinesResult {
  totalCollected: number;
  currency: string;
  lineCount: number;
  invoices: Invoice[];
}
