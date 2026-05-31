export type ServiceKind = 'CONSULTATION' | 'LAB_TEST' | 'PROCEDURE' | 'RADIOLOGY' | 'MEDICINE' | 'WARD' | 'REGISTRATION';

/** Sentinel serviceUid for the singleton REGISTRATION fee (matches the backend). */
export const REGISTRATION_SERVICE_UID = 'DEFAULT';

export const SERVICE_KINDS: { value: ServiceKind; label: string; icon: string }[] = [
  { value: 'CONSULTATION', label: 'Consultation', icon: 'bi-hospital' },
  { value: 'LAB_TEST',     label: 'Lab test',     icon: 'bi-droplet-half' },
  { value: 'PROCEDURE',    label: 'Procedure',    icon: 'bi-scissors' },
  { value: 'RADIOLOGY',    label: 'Radiology',    icon: 'bi-radioactive' },
  { value: 'MEDICINE',     label: 'Medicine',     icon: 'bi-capsule' },
  { value: 'WARD',         label: 'Ward',         icon: 'bi-door-open' },
  { value: 'REGISTRATION', label: 'Registration', icon: 'bi-clipboard-plus' }
];

export interface ServicePrice {
  uid: string;
  planUid: string | null;
  planName: string | null;
  kind: ServiceKind;
  serviceUid: string;
  serviceName: string | null;
  amount: number;
  minAmount: number | null;
  maxAmount: number | null;
  currency: string;
  note: string | null;
  /** Whether this plan covers this service (plan rows only; cash rows report false). */
  covered: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SetServicePriceRequest {
  planUid: string | null;
  kind: ServiceKind;
  serviceUid: string;
  amount: number;
  minAmount: number | null;
  maxAmount: number | null;
  currency: string;
  note: string | null;
  /** Plan rows only: covering a service requires amount > 0 (backend auto-unsets when amount == 0). */
  covered: boolean;
}

export interface UpdateServicePriceRequest {
  amount: number;
  minAmount: number | null;
  maxAmount: number | null;
  note: string | null;
  covered: boolean;
}

export interface ServicePriceSearchParams {
  planUid?: string | null;
  cashOnly?: boolean;
  kind?: ServiceKind;
  serviceUid?: string;
  currency?: string;
  query?: string;
  page?: number;
  size?: number;
  sort?: string;
}
