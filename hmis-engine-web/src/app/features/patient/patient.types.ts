export type Gender = 'MALE' | 'FEMALE' | 'OTHER' | 'UNKNOWN';
export type PaymentType = 'CASH' | 'INSURANCE' | 'MIXED' | 'CORPORATE' | 'EXEMPT';
export type PatientType = 'NEW' | 'RETURNING' | 'REFERRAL' | 'STAFF' | 'DEPENDENT';

export const GENDERS: { value: Gender; label: string }[] = [
  { value: 'MALE',    label: 'Male' },
  { value: 'FEMALE',  label: 'Female' },
  { value: 'OTHER',   label: 'Other' },
  { value: 'UNKNOWN', label: 'Unknown' }
];

export const PAYMENT_TYPES: { value: PaymentType; label: string }[] = [
  { value: 'CASH',      label: 'Cash' },
  { value: 'INSURANCE', label: 'Insurance' },
  { value: 'MIXED',     label: 'Mixed' },
  { value: 'CORPORATE', label: 'Corporate' },
  { value: 'EXEMPT',    label: 'Exempt' }
];

export const PATIENT_TYPES: { value: PatientType; label: string }[] = [
  { value: 'NEW',       label: 'New' },
  { value: 'RETURNING', label: 'Returning' },
  { value: 'REFERRAL',  label: 'Referral' },
  { value: 'STAFF',     label: 'Staff' },
  { value: 'DEPENDENT', label: 'Dependent' }
];

export interface Patient {
  uid: string;
  patientNo: string;

  firstName: string;
  middleName: string | null;
  lastName: string;

  dateOfBirth: string;
  gender: Gender;
  type: PatientType;
  paymentType: PaymentType;

  insurancePlanUid: string | null;
  insurancePlanName: string | null;
  insuranceProviderName: string | null;
  membershipNo: string | null;

  phoneNo: string | null;
  email: string | null;
  address: string | null;
  nationality: string | null;
  nationalId: string | null;
  passportNo: string | null;

  kinFullName: string | null;
  kinRelationship: string | null;
  kinPhoneNo: string | null;

  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PatientSummary {
  uid: string;
  patientNo: string;
  firstName: string;
  middleName: string | null;
  lastName: string;
  dateOfBirth: string;
  gender: Gender;
  paymentType: PaymentType;
  phoneNo: string | null;
  active: boolean;
}

export interface CreatePatientRequest {
  firstName: string;
  middleName: string | null;
  lastName: string;

  dateOfBirth: string;
  gender: Gender;
  type: PatientType;
  paymentType: PaymentType;

  insurancePlanUid: string | null;
  membershipNo: string | null;

  phoneNo: string | null;
  email: string | null;
  address: string | null;
  nationality: string | null;
  nationalId: string | null;
  passportNo: string | null;

  kinFullName: string | null;
  kinRelationship: string | null;
  kinPhoneNo: string | null;
}

export type UpdatePatientRequest = CreatePatientRequest;

export interface PatientSearchParams {
  query?: string;
  active?: boolean;
  gender?: Gender;
  paymentType?: PaymentType;
  page?: number;
  size?: number;
  sort?: string;
}
