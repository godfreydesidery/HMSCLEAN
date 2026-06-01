import { AdmissionStatus } from '../encounter/admission/admission.types';
import { InvoiceLineKind, PaymentMethod } from '../billing/invoice.types';

/** Mirrors ReportingDtos.RevenueByKindEntry. */
export interface RevenueByKindEntry {
  kind: InvoiceLineKind;
  amount: number;
}

/** Mirrors ReportingDtos.RevenueSummaryDto. */
export interface RevenueSummaryDto {
  from: string;
  to: string;
  totalBilled: number;
  totalCollected: number;
  totalCredited: number;
  totalRefunded: number;
  netRevenue: number;
  breakdownByKind: RevenueByKindEntry[];
}

/** Mirrors ReportingDtos.IpdRegisterEntry. */
export interface IpdRegisterEntry {
  admissionUid: string;
  admissionNo: string;
  patientUid: string;
  patientName: string;
  wardUid: string;
  wardName: string;
  bedLabel: string;
  admittingClinicianUsername: string;
  status: string;
  admittedAt: string;
  dischargedAt: string | null;
}

/** Mirrors ReportingDtos.BedOccupancyEntry. */
export interface BedOccupancyEntry {
  wardUid: string;
  wardName: string;
  capacity: number;
  beds: number;
  occupied: number;
  free: number;
  outOfService: number;
}

export type StockLocationKind = 'PHARMACY' | 'STORE';

/** Mirrors ReportingDtos.StockOutEntry. */
export interface StockOutEntry {
  locationUid: string;
  locationName: string;
  locationKind: StockLocationKind;
  medicineUid: string;
  medicineCode: string;
  medicineName: string;
}

/** Mirrors ReportingDtos.ExpiringBatchEntry. */
export interface ExpiringBatchEntry {
  batchUid: string;
  locationUid: string;
  locationName: string;
  locationKind: StockLocationKind;
  medicineUid: string;
  medicineCode: string;
  medicineName: string;
  batchNo: string;
  /** ISO date (LocalDate) — e.g. 2026-05-31. */
  expiresAt: string;
  quantity: number;
}

export interface RevenueReportParams {
  from: string;
  to: string;
}

export interface IpdRegisterParams {
  from: string;
  to: string;
  wardUid?: string;
  status?: AdmissionStatus;
}

// ----- Collections / cash-up (BILL-2) --------------------------------------

/** Mirrors ReportingDtos.MethodAmountEntry. */
export interface MethodAmountEntry {
  method: PaymentMethod;
  amount: number;
  count: number;
}

/** Mirrors ReportingDtos.CashierCollectionEntry. */
export interface CashierCollectionEntry {
  cashierUsername: string;
  cashierName: string | null;
  paymentCount: number;
  totalCollected: number;
  cashCollected: number;
  byMethod: MethodAmountEntry[];
}

/** Mirrors ReportingDtos.CollectionsReportDto. */
export interface CollectionsReportDto {
  from: string;
  to: string;
  totalCollected: number;
  totalCash: number;
  paymentCount: number;
  cashiers: CashierCollectionEntry[];
}

// ----- Revenue by payment mode (BILL-5) ------------------------------------

/** Mirrors ReportingDtos.RevenueByModeDto. */
export interface RevenueByModeDto {
  from: string;
  to: string;
  totalCollected: number;
  byMethod: MethodAmountEntry[];
}

// ----- Pharmacy sales (BILL-5) ---------------------------------------------

/** Mirrors ReportingDtos.PharmacySalesEntry. */
export interface PharmacySalesEntry {
  medicineUid: string;
  medicineCode: string | null;
  medicineName: string | null;
  quantity: number;
  amount: number;
  lineCount: number;
}

/** Mirrors ReportingDtos.PharmacySalesDto. */
export interface PharmacySalesDto {
  from: string;
  to: string;
  totalQuantity: number;
  totalAmount: number;
  items: PharmacySalesEntry[];
}
