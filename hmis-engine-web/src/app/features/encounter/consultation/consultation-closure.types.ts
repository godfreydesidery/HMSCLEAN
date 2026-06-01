export type ClosurePlanKind = 'DISCHARGE' | 'DECEASED' | 'REFERRAL';
export type ClosurePlanStatus = 'PENDING' | 'APPROVED' | 'CANCELLED';
export type ClosureSubjectType = 'ADMISSION' | 'CONSULTATION';

/** Consultation closure kinds — outpatient death or external referral. */
export const CONSULTATION_CLOSURE_KINDS: { value: 'DECEASED' | 'REFERRAL'; label: string; icon: string }[] = [
  { value: 'DECEASED', label: 'Record death', icon: 'bi-heartbreak' },
  { value: 'REFERRAL', label: 'Refer out',    icon: 'bi-box-arrow-up-right' }
];

/**
 * The closure plan DTO (DischargePlanDto) reused for consultations.
 * Keyed by consultation; subjectType is CONSULTATION here.
 */
export interface ClosurePlan {
  uid: string;
  subjectType: ClosureSubjectType;
  admissionUid: string | null;
  admissionNo: string | null;
  consultationUid: string | null;
  consultationNo: string | null;
  kind: ClosurePlanKind;
  status: ClosurePlanStatus;
  history: string | null;
  investigation: string | null;
  management: string | null;
  operationNote: string | null;
  icuNote: string | null;
  recommendations: string | null;
  referralFacility: string | null;
  externalProviderUid: string | null;
  externalProviderName: string | null;
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

export interface CreateClosurePlanRequest {
  kind: 'DECEASED' | 'REFERRAL';
  history: string | null;
  investigation: string | null;
  management: string | null;
  operationNote: string | null;
  icuNote: string | null;
  recommendations: string | null;
  referralFacility: string | null;
  externalProviderUid: string | null;
  referralReason: string | null;
  timeOfDeath: string | null;
  causeOfDeath: string | null;
}

export interface UpdateClosurePlanRequest {
  history: string | null;
  investigation: string | null;
  management: string | null;
  operationNote: string | null;
  icuNote: string | null;
  recommendations: string | null;
  referralFacility: string | null;
  externalProviderUid: string | null;
  referralReason: string | null;
  timeOfDeath: string | null;
  causeOfDeath: string | null;
}
