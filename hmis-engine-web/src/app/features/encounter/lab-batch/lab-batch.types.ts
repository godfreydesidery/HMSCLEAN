/**
 * Mirrors backend `com.otapp.hmis.engine.encounter.labbatch.*` DTOs
 * (Phase 45). Lab batches group N same-test LAB_TEST orders for one
 * bench run; member moves don't change the orders' own statuses.
 */

export type LabBatchStatus = 'OPEN' | 'PROCESSING' | 'COMPLETED' | 'CANCELLED';

export interface LabBatchStatusOption {
  readonly value: LabBatchStatus;
  readonly label: string;
  readonly badgeClass: string;
}

export const LAB_BATCH_STATUSES: readonly LabBatchStatusOption[] = [
  { value: 'OPEN',       label: 'Open',       badgeClass: 'bg-info text-dark' },
  { value: 'PROCESSING', label: 'Processing', badgeClass: 'bg-primary' },
  { value: 'COMPLETED',  label: 'Completed',  badgeClass: 'bg-success' },
  { value: 'CANCELLED',  label: 'Cancelled',  badgeClass: 'bg-secondary' }
];

export interface LabBatch {
  uid: string;
  batchNo: string;
  labTestTypeUid: string;
  labTestCode: string | null;
  labTestName: string | null;
  note: string | null;
  status: LabBatchStatus;
  openedByUsername: string;
  openedAt: string;
  processedAt: string | null;
  completedAt: string | null;
  cancelledAt: string | null;
  cancelReason: string | null;
  memberCount: number;
  memberOrderUids: string[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateLabBatchRequest {
  labTestTypeUid: string;
  note?: string | null;
  orderUids: string[];
}

export interface AddOrderRequest { orderUid: string; }
export interface CancelLabBatchRequest { reason?: string | null; }

/**
 * A LAB_TEST order eligible to join a batch — REQUESTED and not yet a member
 * of any batch. Mirrors backend `LabBatchDtos.BatchableOrderDto`.
 */
export interface BatchableOrder {
  orderUid: string;
  orderNo: string;
  patientUid: string;
  patientNo: string | null;
  patientName: string | null;
  urgency: 'NORMAL' | 'URGENT' | 'STAT';
  requestedAt: string;
}

export interface LabBatchSearchParams {
  status?: LabBatchStatus;
  labTestTypeUid?: string;
  page?: number;
  size?: number;
  sort?: string;
}
