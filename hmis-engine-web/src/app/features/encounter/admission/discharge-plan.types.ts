export type DischargePlanKind = 'DISCHARGE' | 'DECEASED' | 'REFERRAL';
export type DischargePlanStatus = 'PENDING' | 'APPROVED' | 'CANCELLED';

export const DISCHARGE_PLAN_KINDS: { value: DischargePlanKind; label: string }[] = [
  { value: 'DISCHARGE', label: 'Discharge' },
  { value: 'DECEASED',  label: 'Deceased' },
  { value: 'REFERRAL',  label: 'Referral' }
];

export interface DischargePlan {
  uid: string;
  admissionUid: string;
  admissionNo: string | null;
  kind: DischargePlanKind;
  status: DischargePlanStatus;
  history: string | null;
  investigation: string | null;
  management: string | null;
  operationNote: string | null;
  icuNote: string | null;
  recommendations: string | null;
  referralFacility: string | null;
  referralReason: string | null;
  timeOfDeath: string | null;
  causeOfDeath: string | null;
  authoredByUsername: string | null;
  authoredAt: string | null;
  approvedByUsername: string | null;
  approvedAt: string | null;
  cancelledByUsername: string | null;
  cancelledAt: string | null;
  cancelReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface DischargePlanRequest {
  kind?: DischargePlanKind;
  history: string | null;
  investigation: string | null;
  management: string | null;
  operationNote: string | null;
  icuNote: string | null;
  recommendations: string | null;
  referralFacility: string | null;
  referralReason: string | null;
  timeOfDeath: string | null;
  causeOfDeath: string | null;
}
