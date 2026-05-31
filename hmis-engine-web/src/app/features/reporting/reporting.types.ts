import { AdmissionStatus } from '../encounter/admission/admission.types';
import { InvoiceLineKind } from '../billing/invoice.types';

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
