import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { environment } from '../../../../environments/environment';
import { AnalyteTemplate, OrderResult, SaveResultRequest } from './order-result.types';

@Injectable({ providedIn: 'root' })
export class OrderResultService {
  private readonly http = inject(HttpClient);
  private readonly apiBase = `${environment.apiUrl}/encounters/orders`;

  findForOrder(orderUid: string): Observable<OrderResult | null> {
    return this.http
      .get<OrderResult>(`${this.apiBase}/uid/${orderUid}/result`, { observe: 'response' })
      .pipe(
        map((res) => (res.status === 204 ? null : (res.body as OrderResult))),
        catchError((err) => (err?.status === 204 || err?.status === 404 ? of(null) : (() => { throw err; })()))
      );
  }

  /** Analyte definitions for a LAB_TEST order's result-entry grid (empty for non-lab). */
  template(orderUid: string): Observable<AnalyteTemplate[]> {
    return this.http.get<AnalyteTemplate[]>(`${this.apiBase}/uid/${orderUid}/result/template`);
  }

  save(orderUid: string, req: SaveResultRequest): Observable<OrderResult> {
    return this.http.put<OrderResult>(`${this.apiBase}/uid/${orderUid}/result`, req);
  }

  finalize(orderUid: string): Observable<OrderResult> {
    return this.http.post<OrderResult>(`${this.apiBase}/uid/${orderUid}/result/finalize`, {});
  }

  amend(orderUid: string, req: SaveResultRequest): Observable<OrderResult> {
    return this.http.put<OrderResult>(`${this.apiBase}/uid/${orderUid}/result/amend`, req);
  }
}
