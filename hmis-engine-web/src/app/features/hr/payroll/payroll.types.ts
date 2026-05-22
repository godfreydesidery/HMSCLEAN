/** Mirrors backend `com.otapp.hmis.engine.hr.payroll.*` DTOs (Phase 47). */

export type PayrollPeriodStatus = 'DRAFT' | 'VERIFIED' | 'APPROVED' | 'PAID' | 'CANCELLED';

export interface PayrollPeriodStatusOption {
  readonly value: PayrollPeriodStatus;
  readonly label: string;
  readonly badgeClass: string;
}

export const PAYROLL_PERIOD_STATUSES: readonly PayrollPeriodStatusOption[] = [
  { value: 'DRAFT',     label: 'Draft',     badgeClass: 'bg-secondary' },
  { value: 'VERIFIED',  label: 'Verified',  badgeClass: 'bg-primary' },
  { value: 'APPROVED',  label: 'Approved',  badgeClass: 'bg-info text-dark' },
  { value: 'PAID',      label: 'Paid',      badgeClass: 'bg-success' },
  { value: 'CANCELLED', label: 'Cancelled', badgeClass: 'bg-dark' }
];

export interface PayrollPeriod {
  uid: string;
  code: string;
  label: string;
  startDate: string;
  endDate: string;
  currency: string;
  status: PayrollPeriodStatus;
  note: string | null;
  verifiedAt: string | null;
  verifiedByUsername: string | null;
  approvedAt: string | null;
  approvedByUsername: string | null;
  paidAt: string | null;
  cancelledAt: string | null;
  cancelReason: string | null;
  itemCount: number;
  totalNet: string;             // BigDecimal as string
  createdAt: string;
  updatedAt: string;
}

export interface PayrollItemLine {
  uid: string;
  code: string | null;
  name: string;
  type: 'EARNING' | 'DEDUCTION';
  amount: string;
  sortOrder: number;
}

export interface PayrollItem {
  uid: string;
  periodUid: string;
  employeeUid: string;
  employeeNo: string | null;
  employeeName: string | null;
  grossPay: string;
  totalDeductions: string;
  netPay: string;
  paymentMethod: string | null;
  paymentReference: string | null;
  note: string | null;
  lines: PayrollItemLine[];
  createdAt: string;
  updatedAt: string;
}

export interface PayrollPeriodWithItems {
  period: PayrollPeriod;
  items: PayrollItem[];
}

export interface CreatePayrollPeriodRequest {
  code: string;
  label: string;
  startDate: string;
  endDate: string;
  currency?: string;
  note?: string | null;
}

export interface UpsertPayrollItemLineRequest {
  code?: string | null;
  name: string;
  type: 'EARNING' | 'DEDUCTION';
  amount: string;
}

export interface UpsertPayrollItemRequest {
  employeeUid: string;
  grossPay: string;
  totalDeductions: string;
  paymentMethod?: string | null;
  paymentReference?: string | null;
  note?: string | null;
  lines?: UpsertPayrollItemLineRequest[];
}

export interface PayrollSearchParams {
  status?: PayrollPeriodStatus;
  page?: number;
  size?: number;
  sort?: string;
}

export type { EmployeeSummary } from '../employee/employee.types';
