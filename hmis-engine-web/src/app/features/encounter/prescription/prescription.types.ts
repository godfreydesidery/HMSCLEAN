import { MedicineForm } from '../../masterdata/medicines/medicine.types';

export type PrescriptionStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'HELD'
  | 'VERIFIED'
  | 'APPROVED'
  | 'SOLD'
  | 'REJECTED'
  | 'CANCELLED';

export const PRESCRIPTION_STATUSES: { value: PrescriptionStatus; label: string; badgeClass: string }[] = [
  { value: 'PENDING',   label: 'Pending',   badgeClass: 'text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle' },
  { value: 'ACCEPTED',  label: 'Accepted',  badgeClass: 'text-bg-info-subtle text-info-emphasis border border-info-subtle' },
  { value: 'HELD',      label: 'Held',      badgeClass: 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle' },
  { value: 'VERIFIED',  label: 'Verified',  badgeClass: 'text-bg-primary-subtle text-primary border border-primary-subtle' },
  { value: 'APPROVED',  label: 'Approved',  badgeClass: 'text-bg-primary-subtle text-primary border border-primary-subtle' },
  { value: 'SOLD',      label: 'Sold',      badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'REJECTED',  label: 'Rejected',  badgeClass: 'text-bg-danger-subtle text-danger-emphasis border border-danger-subtle' },
  { value: 'CANCELLED', label: 'Cancelled', badgeClass: 'text-bg-light text-secondary border' }
];

export interface Prescription {
  uid: string;
  prescriptionNo: string;
  consultationUid: string;
  patientUid: string;
  medicineUid: string;
  medicineCode: string | null;
  medicineName: string | null;
  medicineStrength: string | null;
  medicineForm: MedicineForm | null;
  status: PrescriptionStatus;
  dose: string;
  frequency: string;
  durationDays: number | null;
  quantity: number | null;
  instructions: string | null;
  requestedAt: string;
  acceptedAt: string | null;
  heldAt: string | null;
  verifiedAt: string | null;
  approvedAt: string | null;
  dispensedAt: string | null;
  rejectedAt: string | null;
  rejectReason: string | null;
  cancelReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePrescriptionRequest {
  medicineUid: string;
  dose: string;
  frequency: string;
  durationDays: number | null;
  quantity: number | null;
  instructions: string | null;
}

/** Mirrors backend `PrescriptionDtos.PrescriptionWorklistRow` — a pharmacy dispensing-queue row. */
export interface PrescriptionWorklistRow {
  uid: string;
  prescriptionNo: string;
  patientUid: string;
  patientNo: string | null;
  patientName: string | null;
  patientClass: 'OUTPATIENT' | 'INPATIENT' | 'OUTSIDER';
  consultationUid: string | null;
  medicineName: string | null;
  dose: string;
  frequency: string;
  quantity: number | null;
  status: PrescriptionStatus;
  settled: boolean;
  requestedAt: string;
}

export interface DispenseWorklistParams {
  status?: PrescriptionStatus;
  patientClass?: 'OUTPATIENT' | 'INPATIENT' | 'OUTSIDER';
  settledOnly?: boolean;
  page?: number;
  size?: number;
}
