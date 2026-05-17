export type ProgressNoteKind = 'DOCTOR' | 'NURSING' | 'OBSERVATION' | 'HANDOVER';

export const PROGRESS_NOTE_KINDS: { value: ProgressNoteKind; label: string; icon: string; badgeClass: string }[] = [
  { value: 'DOCTOR',      label: "Doctor",      icon: 'bi-clipboard2-pulse', badgeClass: 'text-bg-primary-subtle text-primary border border-primary-subtle' },
  { value: 'NURSING',     label: 'Nursing',     icon: 'bi-heart-pulse',      badgeClass: 'text-bg-info-subtle text-info-emphasis border border-info-subtle' },
  { value: 'OBSERVATION', label: 'Observation', icon: 'bi-eye',              badgeClass: 'text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle' },
  { value: 'HANDOVER',    label: 'Handover',    icon: 'bi-arrow-left-right', badgeClass: 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle' }
];

export interface ProgressNote {
  uid: string;
  admissionUid: string;
  kind: ProgressNoteKind;
  authorUsername: string;
  authorName: string | null;
  recordedAt: string;
  body: string;
  deleted: boolean;
  deletedAt: string | null;
  deletedBy: string | null;
  deletedReason: string | null;
}

export interface CreateProgressNoteRequest {
  kind: ProgressNoteKind;
  body: string;
}
