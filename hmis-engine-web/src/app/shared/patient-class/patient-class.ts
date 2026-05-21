/**
 * Patient-class lens for role work queues — mirrors the legacy outpatient /
 * inpatient / outsider list split and the backend `PatientClassScope` enum.
 */
export type PatientClassScope = 'OUTPATIENT' | 'INPATIENT' | 'OUTSIDER';

export const PATIENT_CLASS_SCOPES: { value: PatientClassScope; label: string; badgeClass: string }[] = [
  { value: 'OUTPATIENT', label: 'Outpatient', badgeClass: 'text-bg-primary-subtle text-primary border border-primary-subtle' },
  { value: 'INPATIENT',  label: 'Inpatient',  badgeClass: 'text-bg-info-subtle text-info-emphasis border border-info-subtle' },
  { value: 'OUTSIDER',   label: 'Outsider',   badgeClass: 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle' }
];

export function patientClassLabel(c: PatientClassScope | null | undefined): string {
  return PATIENT_CLASS_SCOPES.find((x) => x.value === c)?.label ?? (c ?? '—');
}

export function patientClassBadgeClass(c: PatientClassScope | null | undefined): string {
  return 'badge ' + (PATIENT_CLASS_SCOPES.find((x) => x.value === c)?.badgeClass ?? 'text-bg-light');
}
