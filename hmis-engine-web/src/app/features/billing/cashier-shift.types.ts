export type CashierShiftStatus = 'OPEN' | 'CLOSED';

export const CASHIER_SHIFT_STATUSES: { value: CashierShiftStatus; label: string; badgeClass: string }[] = [
  { value: 'OPEN',   label: 'Open',   badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'CLOSED', label: 'Closed', badgeClass: 'text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle' }
];

export interface CashierShift {
  uid: string;
  cashierUsername: string;
  currency: string;
  openingFloat: number;
  openedAt: string;
  status: CashierShiftStatus;
  closedAt: string | null;
  /** Cash the cashier physically counted at close. */
  closingDeclaredAmount: number | null;
  /** Opening float + cash takings in the window (system-computed). */
  closingExpectedAmount: number | null;
  /** declared − expected: positive = over, negative = short. */
  variance: number | null;
  closingNote: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface OpenShiftRequest {
  openingFloat: number;
  currency: string | null;
}

export interface CloseShiftRequest {
  closingDeclaredAmount: number;
  note: string | null;
}

export interface CashierShiftSearchParams {
  username?: string;
  status?: CashierShiftStatus;
  page?: number;
  size?: number;
  sort?: string;
}
