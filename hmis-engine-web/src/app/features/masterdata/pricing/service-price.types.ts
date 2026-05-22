export type ServiceKind = 'CONSULTATION' | 'LAB_TEST' | 'PROCEDURE' | 'RADIOLOGY' | 'MEDICINE' | 'WARD';

export const SERVICE_KINDS: { value: ServiceKind; label: string; icon: string }[] = [
  { value: 'CONSULTATION', label: 'Consultation', icon: 'bi-hospital' },
  { value: 'LAB_TEST',     label: 'Lab test',     icon: 'bi-droplet-half' },
  { value: 'PROCEDURE',    label: 'Procedure',    icon: 'bi-scissors' },
  { value: 'RADIOLOGY',    label: 'Radiology',    icon: 'bi-radioactive' },
  { value: 'MEDICINE',     label: 'Medicine',     icon: 'bi-capsule' },
  { value: 'WARD',         label: 'Ward',         icon: 'bi-door-open' }
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
}

export interface UpdateServicePriceRequest {
  amount: number;
  minAmount: number | null;
  maxAmount: number | null;
  note: string | null;
}

export interface ServicePriceSearchParams {
  planUid?: string | null;
  kind?: ServiceKind;
  serviceUid?: string;
  page?: number;
  size?: number;
  sort?: string;
}
