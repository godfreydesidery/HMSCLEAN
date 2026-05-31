import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { environment } from '../../../../environments/environment';
import { SupplierItemPrice } from './medicine-price-lookup.types';

/**
 * Read-only comparison-shopping lookups anchored on a medicine — what each supplier
 * is currently quoting. Backs the "Compare supplier prices" view procurement uses
 * when deciding who to put the next LPO with. Mirrors `MedicinePriceLookupController`.
 */
@Injectable({ providedIn: 'root' })
export class MedicinePriceLookupService {
  private readonly http = inject(HttpClient);
  private base(medicineUid: string): string {
    return `${environment.apiUrl}/procurement/medicines/uid/${medicineUid}/prices`;
  }

  /** Full history across all suppliers — both expired and active quotes. */
  listAll(medicineUid: string): Observable<SupplierItemPrice[]> {
    return this.http.get<SupplierItemPrice[]>(this.base(medicineUid));
  }

  /** Currently-valid quotes only (active AND today within window), cheapest first. */
  listActive(medicineUid: string): Observable<SupplierItemPrice[]> {
    return this.http.get<SupplierItemPrice[]>(`${this.base(medicineUid)}/active`);
  }

  /**
   * Cheapest currently-valid quote across all suppliers. Resolves to {@code null}
   * on 404 — no supplier currently quotes the medicine within its window.
   */
  currentBest(medicineUid: string): Observable<SupplierItemPrice | null> {
    return this.http
      .get<SupplierItemPrice>(`${this.base(medicineUid)}/best`)
      .pipe(catchError(() => of(null)));
  }
}
