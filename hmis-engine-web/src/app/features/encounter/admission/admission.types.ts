import { PaymentType } from '../../patient/patient.types';

export type AdmissionStatus = 'ADMITTED' | 'DISCHARGED' | 'DECEASED' | 'TRANSFERRED' | 'CANCELLED';

export const ADMISSION_STATUSES: { value: AdmissionStatus; label: string; badgeClass: string }[] = [
  { value: 'ADMITTED',    label: 'Admitted',    badgeClass: 'text-bg-primary-subtle text-primary border border-primary-subtle' },
  { value: 'DISCHARGED',  label: 'Discharged',  badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'DECEASED',    label: 'Deceased',    badgeClass: 'text-bg-dark-subtle text-dark-emphasis border border-dark-subtle' },
  { value: 'TRANSFERRED', label: 'Transferred', badgeClass: 'text-bg-info-subtle text-info-emphasis border border-info-subtle' },
  { value: 'CANCELLED',   label: 'Cancelled',   badgeClass: 'text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle' }
];

export interface Admission {
  uid: string;
  admissionNo: string;

  patientUid: string;
  patientNo: string | null;
  patientName: string | null;

  wardUid: string;
  wardName: string | null;
  bedLabel: string | null;

  admittingClinicianUsername: string;
  admittingClinicianName: string | null;

  status: AdmissionStatus;
  paymentType: PaymentType;
  insurancePlanUid: string | null;
  insurancePlanName: string | null;

  consultationUid: string | null;
  consultationNo: string | null;

  admissionReason: string | null;
  admittedAt: string;
  dischargedAt: string | null;
  dischargeSummary: string | null;
  cancelledAt: string | null;
  cancelReason: string | null;

  createdAt: string;
  updatedAt: string;
}

export interface AdmissionSummary {
  uid: string;
  admissionNo: string;
  patientUid: string;
  patientNo: string | null;
  patientName: string | null;
  wardName: string | null;
  bedLabel: string | null;
  admittingClinicianName: string | null;
  status: AdmissionStatus;
  admittedAt: string;
  dischargedAt: string | null;
}

export interface AdmitPatientRequest {
  patientUid: string;
  wardUid: string;
  bedLabel: string | null;
  admittingClinicianUsername: string;
  paymentType: PaymentType;
  insurancePlanUid: string | null;
  consultationUid: string | null;
  admissionReason: string | null;
}

export interface TransferWardRequest {
  wardUid: string;
  bedLabel: string | null;
}

export interface AdmissionSearchParams {
  query?: string;
  status?: AdmissionStatus;
  wardUid?: string;
  patientUid?: string;
  page?: number;
  size?: number;
  sort?: string;
}
