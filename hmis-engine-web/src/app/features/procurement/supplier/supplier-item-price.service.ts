import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { environment } from '../../../../environments/environment';
import {
  CreateSupplierItemPriceRequest, SupplierItemPrice, UpdateSupplierItemPriceRequest
} from './supplier-item-price.types';

/**
 * A supplier's contracted item price list. The PO add-line gate locks onto the
 * supplier's CURRENT quote for a medicine (the legacy findBySupplierAndItem gate).
 */
@Injectable({ providedIn: 'root' })
export class SupplierItemPriceService {
  private readonly http = inject(HttpClient);
  private base(supplierUid: string): string {
    return `${environment.apiUrl}/procurement/suppliers/uid/${supplierUid}/prices`;
  }

  list(supplierUid: string): Observable<SupplierItemPrice[]> {
    return this.http.get<SupplierItemPrice[]>(this.base(supplierUid));
  }

  create(supplierUid: string, req: CreateSupplierItemPriceRequest): Observable<SupplierItemPrice> {
    return this.http.post<SupplierItemPrice>(this.base(supplierUid), req);
  }

  update(supplierUid: string, priceUid: string, req: UpdateSupplierItemPriceRequest): Observable<SupplierItemPrice> {
    return this.http.put<SupplierItemPrice>(`${this.base(supplierUid)}/uid/${priceUid}`, req);
  }

  setActive(supplierUid: string, priceUid: string, active: boolean): Observable<SupplierItemPrice> {
    return this.http.put<SupplierItemPrice>(`${this.base(supplierUid)}/uid/${priceUid}/active`, { active });
  }

  delete(supplierUid: string, priceUid: string): Observable<void> {
    return this.http.delete<void>(`${this.base(supplierUid)}/uid/${priceUid}`);
  }

  /**
   * The supplier's CURRENT contracted quote for one medicine (the price a PO line
   * locks onto). Resolves to {@code null} on 404 — the supplier does not currently
   * quote the medicine, so the add-line gate must refuse it.
   */
  currentForMedicine(supplierUid: string, medicineUid: string): Observable<SupplierItemPrice | null> {
    return this.http
      .get<SupplierItemPrice>(`${this.base(supplierUid)}/medicines/uid/${medicineUid}/current`)
      .pipe(catchError(() => of(null)));
  }
}
