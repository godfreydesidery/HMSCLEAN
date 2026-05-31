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

// ----- dressing chart -------------------------------------------------------

export type WoundStatus = 'CLEAN' | 'HEALING' | 'GRANULATING' | 'SLOUGHY' | 'INFECTED' | 'NECROTIC' | 'DEHISCED';

export const WOUND_STATUSES: { value: WoundStatus; label: string; badgeClass: string }[] = [
  { value: 'CLEAN',       label: 'Clean',       badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'HEALING',     label: 'Healing',     badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'GRANULATING', label: 'Granulating', badgeClass: 'text-bg-primary-subtle text-primary border border-primary-subtle' },
  { value: 'SLOUGHY',     label: 'Sloughy',     badgeClass: 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle' },
  { value: 'INFECTED',    label: 'Infected',    badgeClass: 'text-bg-danger-subtle text-danger-emphasis border border-danger-subtle' },
  { value: 'NECROTIC',    label: 'Necrotic',    badgeClass: 'text-bg-danger-subtle text-danger-emphasis border border-danger-subtle' },
  { value: 'DEHISCED',    label: 'Dehisced',    badgeClass: 'text-bg-danger-subtle text-danger-emphasis border border-danger-subtle' }
];

export interface DressingEntry {
  uid: string;
  admissionUid: string;
  recordedAt: string;
  recordedByUsername: string | null;
  woundLocation: string;
  woundStatus: WoundStatus;
  dressingApplied: string;
  notes: string | null;
  createdAt: string;
}

export interface CreateDressingEntryRequest {
  woundLocation: string;
  woundStatus: WoundStatus;
  dressingApplied: string;
  notes: string | null;
}
