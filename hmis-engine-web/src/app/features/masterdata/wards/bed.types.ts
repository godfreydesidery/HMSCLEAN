export type BedStatus = 'FREE' | 'OCCUPIED' | 'RESERVED' | 'OUT_OF_SERVICE';

export const BED_STATUSES: { value: BedStatus; label: string; badgeClass: string }[] = [
  { value: 'FREE',           label: 'Free',           badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'RESERVED',       label: 'Reserved',       badgeClass: 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle' },
  { value: 'OCCUPIED',       label: 'Occupied',       badgeClass: 'text-bg-primary-subtle text-primary border border-primary-subtle' },
  { value: 'OUT_OF_SERVICE', label: 'Out of service', badgeClass: 'text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle' }
];

export interface Bed {
  uid: string;
  wardUid: string;
  wardName: string | null;
  label: string;
  notes: string | null;
  status: BedStatus;
  occupiedByAdmissionUid: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateBedRequest {
  label: string;
  notes: string | null;
}

export interface UpdateBedRequest {
  label: string;
  notes: string | null;
}
