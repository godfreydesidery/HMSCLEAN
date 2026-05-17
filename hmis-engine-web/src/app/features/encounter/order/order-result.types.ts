import { ClinicalOrderKind } from './clinical-order.types';

export type OrderResultStatus = 'PRELIMINARY' | 'FINAL' | 'AMENDED';

export const ORDER_RESULT_STATUSES: { value: OrderResultStatus; label: string; badgeClass: string }[] = [
  { value: 'PRELIMINARY', label: 'Preliminary', badgeClass: 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle' },
  { value: 'FINAL',       label: 'Final',       badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'AMENDED',     label: 'Amended',     badgeClass: 'text-bg-info-subtle text-info-emphasis border border-info-subtle' }
];

export interface OrderResult {
  uid: string;
  orderUid: string;
  orderKind: ClinicalOrderKind;
  status: OrderResultStatus;
  narrative: string | null;
  impression: string | null;
  finalizedAt: string | null;
  finalizedBy: string | null;
  amendedAt: string | null;
  amendedBy: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SaveResultRequest {
  narrative: string | null;
  impression: string | null;
}
