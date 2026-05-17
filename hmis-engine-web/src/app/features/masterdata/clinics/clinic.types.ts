export type ClinicType = 'OUTPATIENT' | 'INPATIENT' | 'SPECIALTY' | 'EMERGENCY' | 'DAYCARE';

export const CLINIC_TYPES: { value: ClinicType; label: string }[] = [
  { value: 'OUTPATIENT', label: 'Outpatient' },
  { value: 'INPATIENT', label: 'Inpatient' },
  { value: 'SPECIALTY', label: 'Specialty' },
  { value: 'EMERGENCY', label: 'Emergency' },
  { value: 'DAYCARE', label: 'Day care' }
];

export interface Clinic {
  id: number;
  code: string;
  name: string;
  type: ClinicType;
  description: string | null;
  location: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateClinicRequest {
  code: string;
  name: string;
  type: ClinicType;
  description: string | null;
  location: string | null;
}

export interface UpdateClinicRequest {
  name: string;
  type: ClinicType;
  description: string | null;
  location: string | null;
}

export interface ClinicSearchParams {
  query?: string;
  active?: boolean;
  type?: ClinicType;
  page?: number;
  size?: number;
  sort?: string;
}
