export type ClinicalOrderKind = 'LAB_TEST' | 'RADIOLOGY' | 'PROCEDURE';

export const CLINICAL_ORDER_KINDS: { value: ClinicalOrderKind; label: string; icon: string }[] = [
  { value: 'LAB_TEST',  label: 'Lab test',  icon: 'bi-droplet-half' },
  { value: 'RADIOLOGY', label: 'Radiology', icon: 'bi-radioactive' },
  { value: 'PROCEDURE', label: 'Procedure', icon: 'bi-scissors' }
];

export type ClinicalOrderStatus = 'REQUESTED' | 'ACCEPTED' | 'APPROVED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export const CLINICAL_ORDER_STATUSES: { value: ClinicalOrderStatus; label: string; badgeClass: string }[] = [
  { value: 'REQUESTED',   label: 'Requested',   badgeClass: 'text-bg-info-subtle text-info-emphasis border border-info-subtle' },
  { value: 'ACCEPTED',    label: 'Accepted',    badgeClass: 'text-bg-info-subtle text-info-emphasis border border-info-subtle' },
  { value: 'APPROVED',    label: 'Approved',    badgeClass: 'text-bg-primary-subtle text-primary border border-primary-subtle' },
  { value: 'IN_PROGRESS', label: 'In progress', badgeClass: 'text-bg-primary-subtle text-primary border border-primary-subtle' },
  { value: 'COMPLETED',   label: 'Completed',   badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'CANCELLED',   label: 'Cancelled',   badgeClass: 'text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle' }
];

export type OrderUrgency = 'NORMAL' | 'URGENT' | 'STAT';

export const ORDER_URGENCIES: { value: OrderUrgency; label: string; badgeClass: string }[] = [
  { value: 'NORMAL', label: 'Normal', badgeClass: 'text-bg-light border' },
  { value: 'URGENT', label: 'Urgent', badgeClass: 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle' },
  { value: 'STAT',   label: 'STAT',   badgeClass: 'text-bg-danger-subtle text-danger-emphasis border border-danger-subtle' }
];

export interface ClinicalOrder {
  uid: string;
  orderNo: string;
  consultationUid: string;
  patientUid: string;
  kind: ClinicalOrderKind;
  serviceUid: string;
  serviceCode: string | null;
  serviceName: string | null;
  status: ClinicalOrderStatus;
  urgency: OrderUrgency;
  requestedAt: string;
  completedAt: string | null;
  instructions: string | null;
  result: string | null;
  cancelReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateOrderRequest {
  kind: ClinicalOrderKind;
  serviceUid: string;
  urgency: OrderUrgency;
  instructions: string | null;
}
