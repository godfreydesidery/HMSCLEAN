export type AnalyteValueKind = 'NUMERIC' | 'TEXT';
export type RangeSex = 'ANY' | 'MALE' | 'FEMALE';

export const ANALYTE_VALUE_KINDS: { value: AnalyteValueKind; label: string }[] = [
  { value: 'NUMERIC', label: 'Numeric (auto-flagged)' },
  { value: 'TEXT',    label: 'Text / qualitative' }
];

export const RANGE_SEXES: { value: RangeSex; label: string }[] = [
  { value: 'ANY',    label: 'Any' },
  { value: 'MALE',   label: 'Male' },
  { value: 'FEMALE', label: 'Female' }
];

export interface LabReferenceRange {
  id: number;
  uid: string;
  analyteUid: string;
  sex: RangeSex;
  ageMinDays: number | null;
  ageMaxDays: number | null;
  refLow: number | null;
  refHigh: number | null;
  criticalLow: number | null;
  criticalHigh: number | null;
  normalText: string | null;
  rangeDisplay: string | null;
  active: boolean;
}

export interface LabTestAnalyte {
  id: number;
  uid: string;
  labTestTypeUid: string;
  code: string;
  name: string;
  unit: string | null;
  valueKind: AnalyteValueKind;
  displayOrder: number;
  active: boolean;
  ranges: LabReferenceRange[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateAnalyteRequest {
  code: string; name: string; unit: string | null; valueKind: AnalyteValueKind; displayOrder: number;
}
export interface UpdateAnalyteRequest {
  name: string; unit: string | null; valueKind: AnalyteValueKind; displayOrder: number; active: boolean;
}

export interface CreateRangeRequest {
  sex: RangeSex;
  ageMinDays: number | null; ageMaxDays: number | null;
  refLow: number | null; refHigh: number | null; criticalLow: number | null; criticalHigh: number | null;
  normalText: string | null; rangeDisplay: string | null;
}
export interface UpdateRangeRequest extends CreateRangeRequest { active: boolean; }
