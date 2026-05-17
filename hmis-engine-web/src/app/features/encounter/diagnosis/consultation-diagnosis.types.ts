export type DiagnosisKind = 'WORKING' | 'FINAL';

export const DIAGNOSIS_KINDS: { value: DiagnosisKind; label: string; badgeClass: string }[] = [
  { value: 'WORKING', label: 'Working', badgeClass: 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle' },
  { value: 'FINAL',   label: 'Final',   badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' }
];

export interface ConsultationDiagnosis {
  uid: string;
  consultationUid: string;
  kind: DiagnosisKind;
  diagnosisTypeUid: string;
  diagnosisCode: string | null;
  diagnosisName: string | null;
  primaryDiagnosis: boolean;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AddDiagnosisRequest {
  kind: DiagnosisKind;
  diagnosisTypeUid: string;
  primaryDiagnosis: boolean;
  notes: string | null;
}
