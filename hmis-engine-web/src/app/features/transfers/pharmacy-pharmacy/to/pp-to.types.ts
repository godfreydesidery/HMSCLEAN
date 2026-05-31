import { TransferDocStatus } from '../../transfer-common.types';

/* ===========================================================================
 * Inter-pharmacy Transfer Order (TO) — the DELIVERING pharmacy ISSUES stock
 * against an approved/submitted RO raised by the REQUESTING pharmacy.
 * Backend: PharmacyToPharmacyTOController, base /transfers/pharmacy-pharmacy/to.
 * Field names mirror PharmacyPharmacyTransferDtos (TODto / TOLineDto / TOSummary).
 * ======================================================================== */

/* ----- requests ---------------------------------------------------------- */

export interface CreateTOLineRequest {
  roLineUid: string;
  /** What the delivering pharmacy commits to ship for the matching RO line (>= 1). */
  quantity: number;
}

export interface CreateTORequest {
  roUid: string;
  note?: string | null;
  lines: CreateTOLineRequest[];
}

/** Optional reason body for the /reject action. */
export interface ReasonRequest {
  reason?: string | null;
}

/* ----- responses --------------------------------------------------------- */

export interface TOBatchPick {
  batchUid: string;
  batchNo: string;
  expiresAt: string | null;
  quantity: number;
  rnLineUid: string | null;
}

export interface TOLineDto {
  uid: string;
  roLineUid: string;
  medicineUid: string;
  medicineCode: string | null;
  medicineName: string | null;
  medicineStrength: string | null;
  unitUid: string | null;
  unitCode: string | null;
  unitFactorToBase: number;
  requestedQuantity: number;
  issuedQuantity: number;
  receivedQuantity: number;
  picks: TOBatchPick[];
  createdAt: string;
}

export interface TODto {
  uid: string;
  toNo: string;
  roUid: string;
  roNo: string;
  requestingPharmacyUid: string;
  requestingPharmacyName: string | null;
  deliveringPharmacyUid: string;
  deliveringPharmacyName: string | null;
  orderDate: string;
  status: TransferDocStatus;
  verifiedAt: string | null;
  approvedAt: string | null;
  issuedAt: string | null;
  completedAt: string | null;
  rejectedAt: string | null;
  rejectedReason: string | null;
  note: string | null;
  createdAt: string;
  updatedAt: string;
  lines: TOLineDto[];
}

export interface TOSummary {
  uid: string;
  toNo: string;
  roNo: string;
  requestingPharmacyName: string | null;
  deliveringPharmacyName: string | null;
  orderDate: string;
  status: TransferDocStatus;
  lineCount: number;
  createdAt: string;
}

export interface TOSearchParams {
  query?: string;
  status?: TransferDocStatus;
  requestingPharmacyUid?: string;
  deliveringPharmacyUid?: string;
  roUid?: string;
  page?: number;
  size?: number;
  sort?: string;
}

/* ===========================================================================
 * Minimal RO read types — TO create prefills from an RO. Kept inline here so
 * the TO slice stays self-contained (do NOT import the ro/ directory).
 * ======================================================================== */

/** RO row used by the picker in pp-to-create (subset of backend ROSummary). */
export interface RoPickSummary {
  uid: string;
  roNo: string;
  requestingPharmacyName: string | null;
  deliveringPharmacyName: string | null;
  orderDate: string;
  status: TransferDocStatus;
  lineCount: number;
  createdAt: string;
}

/** RO line used to build the TO line FormArray (subset of backend ROLineDto). */
export interface RoPickLine {
  uid: string;
  medicineUid: string;
  medicineCode: string | null;
  medicineName: string | null;
  medicineStrength: string | null;
  unitCode: string | null;
  requestedQuantity: number;
  fulfilledQuantity: number;
  outstandingQuantity: number;
}

/** Full RO read for prefill (subset of backend RODto). */
export interface RoPickDetail {
  uid: string;
  roNo: string;
  requestingPharmacyUid: string;
  requestingPharmacyName: string | null;
  deliveringPharmacyUid: string;
  deliveringPharmacyName: string | null;
  orderDate: string;
  status: TransferDocStatus;
  note: string | null;
  lines: RoPickLine[];
}

export interface RoSearchParams {
  query?: string;
  status?: TransferDocStatus;
  requestingPharmacyUid?: string;
  deliveringPharmacyUid?: string;
  page?: number;
  size?: number;
  sort?: string;
}
