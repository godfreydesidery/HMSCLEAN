import { ReceiveNoteStatus } from '../../transfer-common.types';

// ----- RN create requests --------------------------------------------------

export interface CreateRNLineRequest {
  toLineUid: string;
  /** May be less than the issued amount; the shortfall is recorded on the RN line. */
  receivedQuantity: number;
}

export interface CreateRNRequest {
  toUid: string;
  /** LocalDate (yyyy-MM-dd). Optional — backend defaults to today when null. */
  receivingDate: string | null;
  note: string | null;
  lines: CreateRNLineRequest[];
}

// ----- RN responses --------------------------------------------------------

export interface RNLineDto {
  uid: string;
  toLineUid: string;
  medicineUid: string;
  medicineCode: string | null;
  medicineName: string | null;
  medicineStrength: string | null;
  unitUid: string | null;
  unitCode: string | null;
  unitFactorToBase: number;
  issuedQuantity: number;
  receivedQuantity: number;
  shortfall: number;
  createdAt: string;
}

export interface RNDto {
  uid: string;
  rnNo: string;
  toUid: string;
  toNo: string | null;
  pharmacyUid: string;
  pharmacyName: string | null;
  storeUid: string;
  storeName: string | null;
  /** LocalDate (yyyy-MM-dd). */
  receivingDate: string | null;
  status: ReceiveNoteStatus;
  completedAt: string | null;
  cancelledAt: string | null;
  note: string | null;
  createdAt: string;
  updatedAt: string;
  lines: RNLineDto[];
}

export interface RNSummary {
  uid: string;
  rnNo: string;
  toNo: string | null;
  pharmacyName: string | null;
  storeName: string | null;
  receivingDate: string | null;
  status: ReceiveNoteStatus;
  lineCount: number;
  createdAt: string;
}

export interface RNSearchParams {
  query?: string;
  status?: ReceiveNoteStatus;
  pharmacyUid?: string;
  storeUid?: string;
  toUid?: string;
  page?: number;
  size?: number;
  sort?: string;
}

// ----- minimal TO read types (prefill source; self-contained — do NOT import to/) -----

/** Subset of the backend TOSummary used only to pick a GOODS_ISSUED TO. */
export interface ToPickSummary {
  uid: string;
  toNo: string;
  roNo: string | null;
  pharmacyName: string | null;
  storeName: string | null;
  createdAt: string;
}

/** Subset of the backend TOLineDto used to prefill RN lines. */
export interface ToPickLine {
  uid: string;
  medicineName: string | null;
  issuedQuantity: number;
  receivedQuantity: number;
}

/** Subset of the backend TODto used to prefill an RN from an issued TO. */
export interface ToPickDetail {
  uid: string;
  toNo: string;
  roNo: string | null;
  pharmacyUid: string;
  pharmacyName: string | null;
  storeUid: string;
  storeName: string | null;
  lines: ToPickLine[];
}

export interface ToPickSearchParams {
  query?: string;
  page?: number;
  size?: number;
  sort?: string;
}
