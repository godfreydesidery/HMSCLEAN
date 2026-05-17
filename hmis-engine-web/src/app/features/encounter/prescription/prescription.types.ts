import { MedicineForm } from '../../masterdata/medicines/medicine.types';

export type PrescriptionStatus = 'REQUESTED' | 'DISPENSED' | 'CANCELLED';

export const PRESCRIPTION_STATUSES: { value: PrescriptionStatus; label: string; badgeClass: string }[] = [
  { value: 'REQUESTED', label: 'Requested', badgeClass: 'text-bg-info-subtle text-info-emphasis border border-info-subtle' },
  { value: 'DISPENSED', label: 'Dispensed', badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'CANCELLED', label: 'Cancelled', badgeClass: 'text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle' }
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
  dispensedAt: string | null;
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
