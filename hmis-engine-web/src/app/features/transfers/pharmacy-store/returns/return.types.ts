/** Lifecycle of a pharmacy→store return (backend PharmacyStoreReturnStatus). */
export type ReturnStatus = 'DRAFT' | 'SUBMITTED' | 'COMPLETED' | 'REJECTED' | 'CANCELLED';

export const RETURN_STATUSES: { value: ReturnStatus; label: string; badgeClass: string }[] = [
  { value: 'DRAFT',     label: 'Draft',     badgeClass: 'text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle' },
  { value: 'SUBMITTED', label: 'Submitted', badgeClass: 'text-bg-primary-subtle text-primary border border-primary-subtle' },
  { value: 'COMPLETED', label: 'Completed', badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'REJECTED',  label: 'Rejected',  badgeClass: 'text-bg-danger-subtle text-danger-emphasis border border-danger-subtle' },
  { value: 'CANCELLED', label: 'Cancelled', badgeClass: 'text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle' }
];

export function returnBadgeClass(s: ReturnStatus): string {
  return 'badge ' + (RETURN_STATUSES.find((x) => x.value === s)?.badgeClass ?? '');
}
export function returnLabel(s: ReturnStatus): string {
  return RETURN_STATUSES.find((x) => x.value === s)?.label ?? s;
}

/** A FEFO batch pick recorded against a return line (populated on completion). */
export interface BatchPickDto {
  batchUid: string;
  batchNo: string | null;
  expiresAt: string | null;
  quantity: number;
}

/** One line on a return. Quantity is in the line's unit (or base if no unit). */
export interface ReturnLineDto {
  uid: string;
  medicineUid: string;
  medicineCode: string | null;
  medicineName: string | null;
  medicineStrength: string | null;
  unitUid: string | null;
  unitCode: string | null;
  unitFactorToBase: number;
  quantity: number;
  reason: string | null;
  picks: BatchPickDto[];
  createdAt: string;
}

export interface ReturnDto {
  uid: string;
  returnNo: string;
  pharmacyUid: string;
  pharmacyName: string | null;
  storeUid: string;
  storeName: string | null;
  returnDate: string | null;
  reason: string | null;
  note: string | null;
  status: ReturnStatus;
  submittedAt: string | null;
  submittedByUsername: string | null;
  completedAt: string | null;
  completedByUsername: string | null;
  rejectedAt: string | null;
  rejectedByUsername: string | null;
  rejectReason: string | null;
  cancelledAt: string | null;
  createdAt: string;
  updatedAt: string;
  lines: ReturnLineDto[];
}

export interface ReturnSummary {
  uid: string;
  returnNo: string;
  pharmacyName: string | null;
  storeName: string | null;
  returnDate: string | null;
  status: ReturnStatus;
  lineCount: number;
  createdAt: string;
}

export interface CreateReturnLineRequest {
  medicineUid: string;
  unitUid: string | null;
  quantity: number;
  reason: string | null;
}

export interface CreateReturnRequest {
  pharmacyUid: string;
  storeUid: string;
  returnDate: string | null;
  reason: string | null;
  note: string | null;
  lines: CreateReturnLineRequest[];
}

export interface ReturnSearchParams {
  query?: string;
  status?: ReturnStatus;
  pharmacyUid?: string;
  storeUid?: string;
  page?: number;
  size?: number;
  sort?: string;
}
