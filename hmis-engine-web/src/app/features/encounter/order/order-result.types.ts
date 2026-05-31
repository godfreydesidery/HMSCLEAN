import { ClinicalOrderKind } from './clinical-order.types';

export type OrderResultStatus = 'PRELIMINARY' | 'FINAL' | 'AMENDED';

export const ORDER_RESULT_STATUSES: { value: OrderResultStatus; label: string; badgeClass: string }[] = [
  { value: 'PRELIMINARY', label: 'Preliminary', badgeClass: 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle' },
  { value: 'FINAL',       label: 'Final',       badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'AMENDED',     label: 'Amended',     badgeClass: 'text-bg-info-subtle text-info-emphasis border border-info-subtle' }
];

export type AnalyteValueKind = 'NUMERIC' | 'TEXT';

export type LabResultFlag =
  'NONE' | 'NORMAL' | 'LOW' | 'HIGH' | 'CRITICAL_LOW' | 'CRITICAL_HIGH' | 'ABNORMAL';

/** Display metadata for a computed abnormal flag. */
export const LAB_RESULT_FLAGS: Record<LabResultFlag, { label: string; badgeClass: string; abnormal: boolean }> = {
  NONE:          { label: '—',         badgeClass: 'text-bg-light text-muted border',                                          abnormal: false },
  NORMAL:        { label: 'Normal',    badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle', abnormal: false },
  LOW:           { label: 'Low',       badgeClass: 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle', abnormal: true },
  HIGH:          { label: 'High',      badgeClass: 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle', abnormal: true },
  CRITICAL_LOW:  { label: 'Crit. low', badgeClass: 'text-bg-danger-subtle text-danger-emphasis border border-danger-subtle',    abnormal: true },
  CRITICAL_HIGH: { label: 'Crit. high',badgeClass: 'text-bg-danger-subtle text-danger-emphasis border border-danger-subtle',    abnormal: true },
  ABNORMAL:      { label: 'Abnormal',  badgeClass: 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle', abnormal: true }
};

/** Analyte definition used to build a result-entry grid (no patient values). */
export interface AnalyteTemplate {
  uid: string;
  code: string;
  name: string;
  unit: string | null;
  valueKind: AnalyteValueKind;
  displayOrder: number;
}

/** A measured analyte value on a result, with the snapshotted range + computed flag. */
export interface LabResultLine {
  id: number;
  uid: string;
  analyteUid: string;
  analyteCode: string;
  analyteName: string;
  valueKind: AnalyteValueKind;
  valueNumeric: number | null;
  valueText: string | null;
  unit: string | null;
  flag: LabResultFlag;
  refLow: number | null;
  refHigh: number | null;
  criticalLow: number | null;
  criticalHigh: number | null;
  rangeDisplay: string | null;
  displayOrder: number;
  note: string | null;
}

export interface OrderResult {
  id: number;
  uid: string;
  orderUid: string;
  orderKind: ClinicalOrderKind;
  status: OrderResultStatus;
  narrative: string | null;
  impression: string | null;
  lines: LabResultLine[];
  finalizedAt: string | null;
  finalizedBy: string | null;
  amendedAt: string | null;
  amendedBy: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface LabResultLineInput {
  analyteUid: string;
  valueNumeric: number | null;
  valueText: string | null;
}

export interface SaveResultRequest {
  narrative: string | null;
  impression: string | null;
  lines: LabResultLineInput[] | null;
}
