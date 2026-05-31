import { TransferDocStatus } from '../../transfer-common.types';

/** One line on a Requisition Order. Quantities are in the line's requested unit (or base if no unit). */
export interface ROLineDto {
  uid: string;
  medicineUid: string;
  medicineCode: string | null;
  medicineName: string | null;
  medicineStrength: string | null;
  unitUid: string | null;
  unitCode: string | null;
  unitFactorToBase: number;
  requestedQuantity: number;
  fulfilledQuantity: number;
  outstandingQuantity: number;
  note: string | null;
  createdAt: string;
}

export interface RODto {
  uid: string;
  roNo: string;
  pharmacyUid: string;
  pharmacyName: string | null;
  storeUid: string;
  storeName: string | null;
  orderDate: string;
  validUntil: string | null;
  status: TransferDocStatus;
  verifiedAt: string | null;
  approvedAt: string | null;
  submittedAt: string | null;
  inProcessAt: string | null;
  issuedAt: string | null;
  completedAt: string | null;
  rejectedAt: string | null;
  returnedAt: string | null;
  rejectReason: string | null;
  note: string | null;
  createdAt: string;
  updatedAt: string;
  lines: ROLineDto[];
}

export interface ROSummary {
  uid: string;
  roNo: string;
  pharmacyName: string | null;
  storeName: string | null;
  orderDate: string;
  status: TransferDocStatus;
  lineCount: number;
  createdAt: string;
}

export interface CreateROLineRequest {
  medicineUid: string;
  unitUid: string | null;
  quantity: number;
  note: string | null;
}

export interface CreateRORequest {
  pharmacyUid: string;
  storeUid: string;
  validUntil: string | null;
  note: string | null;
  lines: CreateROLineRequest[];
}

export interface ROSearchParams {
  query?: string;
  status?: TransferDocStatus;
  pharmacyUid?: string;
  storeUid?: string;
  page?: number;
  size?: number;
  sort?: string;
}
