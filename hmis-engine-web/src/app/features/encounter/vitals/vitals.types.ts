import { ConsultationStatus } from '../consultation/consultation.types';

/**
 * Outpatient vitals capture lifecycle (OPC-3). The row is materialised EMPTY the
 * moment an encounter needs vitals; the nurse fills it (PENDING) then submits it
 * (SUBMITTED, locked); the doctor consumes it into the exam (ARCHIVED, terminal).
 */
export type VitalsStatus = 'EMPTY' | 'PENDING' | 'SUBMITTED' | 'ARCHIVED';

export interface PatientVitals {
  uid: string;
  consultationUid: string;
  patientUid: string;
  status: VitalsStatus;
  takenAt: string;
  temperatureC: number | null;
  pulseBpm: number | null;
  respirationBpm: number | null;
  bloodPressureSystolic: number | null;
  bloodPressureDiastolic: number | null;
  spo2Percent: number | null;
  weightKg: number | null;
  heightCm: number | null;
  notes: string | null;
  submittedAt: string | null;
  archivedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface RecordVitalsRequest {
  temperatureC: number | null;
  pulseBpm: number | null;
  respirationBpm: number | null;
  bloodPressureSystolic: number | null;
  bloodPressureDiastolic: number | null;
  spo2Percent: number | null;
  weightKg: number | null;
  heightCm: number | null;
  notes: string | null;
}

/**
 * One row of the outpatient nurse-triage worklist: a fee-settled consultation
 * (BOOKED / IN_PROGRESS) that may still need vitals, enriched with the patient
 * identity and the current vitals-capture status (null when no row exists yet).
 */
export interface VitalsWorklistRow {
  consultationId: number;
  consultationUid: string;
  consultationNo: string | null;
  patientUid: string | null;
  patientNo: string | null;
  patientName: string | null;
  consultationStatus: ConsultationStatus;
  feeSettled: boolean;
  vitalsUid: string | null;
  vitalsStatus: VitalsStatus | null;
  bookedAt: string | null;
  startedAt: string | null;
}
