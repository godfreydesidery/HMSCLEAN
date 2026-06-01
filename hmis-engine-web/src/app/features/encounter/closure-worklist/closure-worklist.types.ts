import { DischargePlanKind, DischargePlanStatus } from '../admission/discharge-plan.types';

export type ClosureSubject = 'ADMISSION' | 'CONSULTATION';

/** A PENDING closure plan awaiting a second approver (DISCH-1). */
export interface ClosureWorklistItem {
  uid: string;
  subjectType: ClosureSubject;
  kind: DischargePlanKind;
  status: DischargePlanStatus;
  admissionUid: string | null;
  admissionNo: string | null;
  consultationUid: string | null;
  consultationNo: string | null;
  patientUid: string | null;
  patientNo: string | null;
  patientName: string | null;
  referralFacility: string | null;
  authoredByUsername: string | null;
  authoredAt: string | null;
}

export const CLOSURE_SUBJECT_FILTERS: { value: ClosureSubject | ''; label: string }[] = [
  { value: '',             label: 'All closures' },
  { value: 'ADMISSION',    label: 'Inpatient' },
  { value: 'CONSULTATION', label: 'Outpatient' }
];
