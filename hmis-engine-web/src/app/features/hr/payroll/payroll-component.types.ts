/** Mirrors backend `com.otapp.hmis.engine.hr.payroll.*` component DTOs (section E/F). */

export type PayrollComponentType = 'EARNING' | 'DEDUCTION';
export type PayrollCalcMethod = 'FIXED' | 'PERCENT' | 'BAND';
export type PayrollCalcBase = 'BASIC' | 'GROSS';

export const COMPONENT_TYPES: { value: PayrollComponentType; label: string; badgeClass: string }[] = [
  { value: 'EARNING',   label: 'Earning',   badgeClass: 'bg-success' },
  { value: 'DEDUCTION', label: 'Deduction', badgeClass: 'bg-danger' }
];

export const CALC_METHODS: { value: PayrollCalcMethod; label: string }[] = [
  { value: 'FIXED',   label: 'Fixed amount' },
  { value: 'PERCENT', label: 'Percentage of base' },
  { value: 'BAND',    label: 'Progressive bands' }
];

export const CALC_BASES: { value: PayrollCalcBase; label: string }[] = [
  { value: 'BASIC', label: 'Basic pay' },
  { value: 'GROSS', label: 'Gross (basic + earnings)' }
];

export interface PayrollComponentBand {
  uid?: string;
  sortOrder: number;
  fromAmount: string;
  toAmount: string | null;
  rate: string;            // fraction, 0.1 = 10%
}

export interface PayrollComponent {
  uid: string;
  code: string;
  name: string;
  type: PayrollComponentType;
  method: PayrollCalcMethod;
  base: PayrollCalcBase;
  fixedAmount: string | null;
  percentRate: string | null;
  active: boolean;
  sortOrder: number;
  bands: PayrollComponentBand[];
  createdAt: string;
  updatedAt: string;
}

export interface BandRequest {
  fromAmount: string;
  toAmount?: string | null;
  rate: string;
}

export interface CreatePayrollComponentRequest {
  code: string;
  name: string;
  type: PayrollComponentType;
  method: PayrollCalcMethod;
  base?: PayrollCalcBase;
  fixedAmount?: string | null;
  percentRate?: string | null;
  active?: boolean;
  sortOrder?: number;
  bands?: BandRequest[];
}

export interface UpdatePayrollComponentRequest {
  name: string;
  type: PayrollComponentType;
  method: PayrollCalcMethod;
  base?: PayrollCalcBase;
  fixedAmount?: string | null;
  percentRate?: string | null;
  active: boolean;
  sortOrder?: number;
  bands?: BandRequest[];
}

export interface PayrollComponentSearchParams {
  active?: boolean;
  type?: PayrollComponentType;
  page?: number;
  size?: number;
  sort?: string;
}

export interface ComputePayrollRequest {
  basicSalary: string;
  workedDays?: number | null;
  periodDays?: number | null;
}

export interface ComputedLine {
  componentUid: string;
  code: string;
  name: string;
  type: PayrollComponentType;
  method: PayrollCalcMethod;
  amount: string;
}

export interface ComputedPayroll {
  inputBasic: string;
  effectiveBasic: string;
  workedDays: number | null;
  periodDays: number | null;
  totalEarnings: string;
  grossPay: string;
  totalDeductions: string;
  netPay: string;
  lines: ComputedLine[];
}
