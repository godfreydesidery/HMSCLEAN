/**
 * Mirrors backend `SupplierItemPriceDtos.SupplierItemPriceDto` as returned by
 * `MedicinePriceLookupController` (`/procurement/medicines/uid/{medicineUid}/prices`).
 * Read-only, comparison-shopping shape: each row is one supplier's quote for the medicine.
 */
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
