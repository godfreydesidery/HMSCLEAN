export type ClaimStatus = 'DRAFT' | 'SUBMITTED' | 'PARTIALLY_SETTLED' | 'SETTLED' | 'REJECTED';

export const CLAIM_STATUSES: { value: ClaimStatus; label: string; badgeClass: string }[] = [
  { value: 'DRAFT',             label: 'Draft',             badgeClass: 'text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle' },
  { value: 'SUBMITTED',         label: 'Submitted',         badgeClass: 'text-bg-info-subtle text-info-emphasis border border-info-subtle' },
  { value: 'PARTIALLY_SETTLED', label: 'Partially settled', badgeClass: 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle' },
  { value: 'SETTLED',           label: 'Settled',           badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'REJECTED',          label: 'Rejected',          badgeClass: 'text-bg-danger-subtle text-danger-emphasis border border-danger-subtle' }
];

export interface ClaimLine {
  id: number;
  uid: string;
  invoiceLineId: number;
  serviceUid: string | null;
  kind: string;
  description: string;
  quantity: number;
  unitPrice: number;
  amount: number;
}

export interface Claim {
  id: number;
  uid: string;
  claimNo: string;
  payerPlanUid: string;
  payerPlanName: string;
  providerUid: string | null;
  providerName: string | null;
  membershipNo: string;
  patientUid: string;
  currency: string;
  claimedAmount: number;
  settledAmount: number;
  outstanding: number;
  status: ClaimStatus;
  lineCount: number;
  submittedAt: string | null;
  settledAt: string | null;
  rejectedAt: string | null;
  rejectionReason: string | null;
  submittedByUsername: string | null;
  settledByUsername: string | null;
  rejectedByUsername: string | null;
  createdAt: string;
  lines: ClaimLine[];
}

export interface ClaimSummary {
  id: number;
  uid: string;
  claimNo: string;
  payerPlanUid: string;
  payerPlanName: string;
  providerName: string | null;
  membershipNo: string;
  patientUid: string;
  currency: string;
  claimedAmount: number;
  settledAmount: number;
  status: ClaimStatus;
  lineCount: number;
  createdAt: string;
}

export interface AssembleClaimRequest {
  payerPlanUid: string;
  membershipNo: string;
}

export interface RecordSettlementRequest {
  amount: number;
  reference: string | null;
  note: string | null;
}

export interface RejectClaimRequest {
  reason: string;
}

export interface ClaimSearchParams {
  status?: ClaimStatus;
  payerPlanUid?: string;
  providerUid?: string;
  membershipNo?: string;
  page?: number;
  size?: number;
  sort?: string;
}
