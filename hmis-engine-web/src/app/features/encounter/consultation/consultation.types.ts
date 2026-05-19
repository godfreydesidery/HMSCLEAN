import { PaymentType } from '../../patient/patient.types';

export type ConsultationStatus = 'BOOKED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'TRANSFERRED';

export const CONSULTATION_STATUSES: { value: ConsultationStatus; label: string; badgeClass: string }[] = [
  { value: 'BOOKED',      label: 'Booked',      badgeClass: 'text-bg-info-subtle text-info-emphasis border border-info-subtle' },
  { value: 'IN_PROGRESS', label: 'In progress', badgeClass: 'text-bg-primary-subtle text-primary border border-primary-subtle' },
  { value: 'COMPLETED',   label: 'Completed',   badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'CANCELLED',   label: 'Cancelled',   badgeClass: 'text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle' },
  { value: 'TRANSFERRED', label: 'Transferred', badgeClass: 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle' }
];

export interface Consultation {
  uid: string;
  consultationNo: string;

  patientUid: string;
  patientNo: string | null;
  patientName: string | null;

  clinicUid: string;
  clinicName: string | null;

  clinicianUsername: string;
  clinicianName: string | null;

  status: ConsultationStatus;
  paymentType: PaymentType;
  insurancePlanUid: string | null;
  insurancePlanName: string | null;

  reason: string | null;
  bookedAt: string;
  startedAt: string | null;
  completedAt: string | null;
  cancelledAt: string | null;
  cancelReason: string | null;

  // Phase 44 linkages
  followUpOfConsultationUid: string | null;
  transferredToConsultationUid: string | null;
  transferredFromConsultationUid: string | null;
  transferReason: string | null;
  transferredAt: string | null;

  createdAt: string;
  updatedAt: string;
}

export interface ConsultationSummary {
  uid: string;
  consultationNo: string;
  patientUid: string;
  patientNo: string | null;
  patientName: string | null;
  clinicName: string | null;
  clinicianName: string | null;
  status: ConsultationStatus;
  bookedAt: string;
  startedAt: string | null;
}

export interface StartConsultationRequest {
  patientUid: string;
  clinicUid: string;
  clinicianUsername: string;
  paymentType: PaymentType;
  insurancePlanUid: string | null;
  reason: string | null;
  /** Optional — references a prior consultation to mark this visit as a follow-up. */
  followUpOfConsultationUid?: string | null;
}

export interface TransferConsultationRequest {
  targetClinicUid: string;
  targetClinicianUsername: string;
  reason?: string | null;
}

export interface ConsultationSearchParams {
  query?: string;
  status?: ConsultationStatus;
  clinicUid?: string;
  patientUid?: string;
  clinicianUsername?: string;
  page?: number;
  size?: number;
  sort?: string;
}
