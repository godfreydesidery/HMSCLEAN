/** Shared transfer-document status vocabulary (backend TransferDocStatus / ReceiveNoteStatus). */

export type TransferDocStatus =
  | 'PENDING' | 'VERIFIED' | 'APPROVED' | 'SUBMITTED' | 'IN_PROCESS'
  | 'GOODS_ISSUED' | 'COMPLETED' | 'REJECTED' | 'RETURNED';

export const TRANSFER_DOC_STATUSES: { value: TransferDocStatus; label: string; badgeClass: string }[] = [
  { value: 'PENDING',      label: 'Pending',      badgeClass: 'text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle' },
  { value: 'VERIFIED',     label: 'Verified',     badgeClass: 'text-bg-info-subtle text-info-emphasis border border-info-subtle' },
  { value: 'APPROVED',     label: 'Approved',     badgeClass: 'text-bg-primary-subtle text-primary border border-primary-subtle' },
  { value: 'SUBMITTED',    label: 'Submitted',    badgeClass: 'text-bg-primary-subtle text-primary border border-primary-subtle' },
  { value: 'IN_PROCESS',   label: 'In process',   badgeClass: 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle' },
  { value: 'GOODS_ISSUED', label: 'Goods issued', badgeClass: 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle' },
  { value: 'COMPLETED',    label: 'Completed',    badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'REJECTED',     label: 'Rejected',     badgeClass: 'text-bg-danger-subtle text-danger-emphasis border border-danger-subtle' },
  { value: 'RETURNED',     label: 'Returned',     badgeClass: 'text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle' }
];

export function transferDocBadgeClass(s: TransferDocStatus): string {
  return 'badge ' + (TRANSFER_DOC_STATUSES.find((x) => x.value === s)?.badgeClass ?? '');
}
export function transferDocLabel(s: TransferDocStatus): string {
  return TRANSFER_DOC_STATUSES.find((x) => x.value === s)?.label ?? s;
}

export type ReceiveNoteStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED';

export const RECEIVE_NOTE_STATUSES: { value: ReceiveNoteStatus; label: string; badgeClass: string }[] = [
  { value: 'PENDING',   label: 'Pending',   badgeClass: 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle' },
  { value: 'COMPLETED', label: 'Completed', badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'CANCELLED', label: 'Cancelled', badgeClass: 'text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle' }
];

export function receiveNoteBadgeClass(s: ReceiveNoteStatus): string {
  return 'badge ' + (RECEIVE_NOTE_STATUSES.find((x) => x.value === s)?.badgeClass ?? '');
}
export function receiveNoteLabel(s: ReceiveNoteStatus): string {
  return RECEIVE_NOTE_STATUSES.find((x) => x.value === s)?.label ?? s;
}
