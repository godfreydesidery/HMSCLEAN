import { TransferDocStatus } from '../../transfer-common.types';

/** One line on an inter-pharmacy Requisition Order. Quantities are in the line's requested unit (or base if no unit). */
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
  requestingPharmacyUid: string;
  requestingPharmacyName: string | null;
  deliveringPharmacyUid: string;
  deliveringPharmacyName: string | null;
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
  requestingPharmacyName: string | null;
  deliveringPharmacyName: string | null;
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
  requestingPharmacyUid: string;
  deliveringPharmacyUid: string;
  validUntil: string | null;
  note: string | null;
  lines: CreateROLineRequest[];
}

export interface ROSearchParams {
  query?: string;
  status?: TransferDocStatus;
  requestingPharmacyUid?: string;
  deliveringPharmacyUid?: string;
  page?: number;
  size?: number;
  sort?: string;
}
