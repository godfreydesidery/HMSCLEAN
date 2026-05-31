/** Mirrors backend `SupplierItemPriceDtos` — a supplier's contracted quote for a medicine. */

export interface SupplierItemPrice {
  uid: string;
  supplierUid: string;
  supplierName: string | null;
  medicineUid: string;
  medicineCode: string | null;
  medicineName: string | null;
  medicineStrength: string | null;
  unitPrice: number;
  currency: string;
  validFrom: string;
  validTo: string | null;
  active: boolean;
  /** Active AND today within [validFrom, validTo] — the quote a PO line will lock onto. */
  currentlyValid: boolean;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSupplierItemPriceRequest {
  medicineUid: string;
  unitPrice: number;
  currency: string;
  validFrom: string;
  validTo: string | null;
  notes: string | null;
}

export interface UpdateSupplierItemPriceRequest {
  unitPrice: number;
  currency: string;
  validTo: string | null;
  notes: string | null;
}
