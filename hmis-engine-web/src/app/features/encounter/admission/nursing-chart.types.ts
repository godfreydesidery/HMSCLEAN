/** Mirrors backend `com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos`. */

// ----- vitals ---------------------------------------------------------------

export interface VitalsEntry {
  uid: string;
  admissionUid: string;
  recordedAt: string;
  recordedByUsername: string | null;
  temperatureC: number | null;
  pulseBpm: number | null;
  respirationsBpm: number | null;
  systolicBp: number | null;
  diastolicBp: number | null;
  spo2Percent: number | null;
  bloodGlucoseMmol: number | null;
  painScore: number | null;
  notes: string | null;
  createdAt: string;
}

export interface CreateVitalsEntryRequest {
  temperatureC: number | null;
  pulseBpm: number | null;
  respirationsBpm: number | null;
  systolicBp: number | null;
  diastolicBp: number | null;
  spo2Percent: number | null;
  bloodGlucoseMmol: number | null;
  painScore: number | null;
  notes: string | null;
}

// ----- care plan ------------------------------------------------------------

export type CarePlanStatus = 'ACTIVE' | 'RESOLVED' | 'CANCELLED';

export const CARE_PLAN_STATUSES: { value: CarePlanStatus; label: string; badgeClass: string }[] = [
  { value: 'ACTIVE',    label: 'Active',    badgeClass: 'text-bg-primary-subtle text-primary border border-primary-subtle' },
  { value: 'RESOLVED',  label: 'Resolved',  badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'CANCELLED', label: 'Cancelled', badgeClass: 'text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle' }
];

export interface CarePlanItem {
  uid: string;
  admissionUid: string;
  problem: string;
  goal: string;
  intervention: string;
  evaluation: string | null;
  status: CarePlanStatus;
  openedByUsername: string | null;
  openedAt: string | null;
  closedByUsername: string | null;
  closedAt: string | null;
  closeReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SaveCarePlanItemRequest {
  problem: string;
  goal: string;
  intervention: string;
  evaluation: string | null;
}
