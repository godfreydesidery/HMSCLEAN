import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import { StockMovement } from '../stock/stock.types';
import {
  CreatePharmacySaleOrderRequest, PharmacySaleOrder, PharmacySaleOrderSearchParams,
  PharmacySaleOrderSummary
} from './pharmacy-sale.types';

@Injectable({ providedIn: 'root' })
export class PharmacySaleOrderService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/pharmacy/sales`;
  private readonly pharmacyBase = `${environment.apiUrl}/pharmacy`;

  search(params: PharmacySaleOrderSearchParams = {}): Observable<PageResponse<PharmacySaleOrderSummary>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.status) p = p.set('status', params.status);
    if (params.pharmacyUid) p = p.set('pharmacyUid', params.pharmacyUid);
    if (params.patientUid) p = p.set('patientUid', params.patientUid);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<PharmacySaleOrderSummary>>(this.base, { params: p });
  }

  findByUid(saleUid: string): Observable<PharmacySaleOrder> {
    return this.http.get<PharmacySaleOrder>(`${this.base}/uid/${saleUid}`);
  }

  create(req: CreatePharmacySaleOrderRequest): Observable<PharmacySaleOrder> {
    return this.http.post<PharmacySaleOrder>(this.base, req);
  }

  cancel(saleUid: string, reason: string | null): Observable<PharmacySaleOrder> {
    return this.http.post<PharmacySaleOrder>(`${this.base}/uid/${saleUid}/cancel`, { reason });
  }

  // ---- per-line transitions
  acceptLine(saleUid: string, lineUid: string): Observable<PharmacySaleOrder> {
    return this.http.post<PharmacySaleOrder>(`${this.base}/uid/${saleUid}/lines/uid/${lineUid}/accept`, {});
  }
  holdLine(saleUid: string, lineUid: string): Observable<PharmacySaleOrder> {
    return this.http.post<PharmacySaleOrder>(`${this.base}/uid/${saleUid}/lines/uid/${lineUid}/hold`, {});
  }
  verifyLine(saleUid: string, lineUid: string): Observable<PharmacySaleOrder> {
    return this.http.post<PharmacySaleOrder>(`${this.base}/uid/${saleUid}/lines/uid/${lineUid}/verify`, {});
  }
  approveLine(saleUid: string, lineUid: string): Observable<PharmacySaleOrder> {
    return this.http.post<PharmacySaleOrder>(`${this.base}/uid/${saleUid}/lines/uid/${lineUid}/approve`, {});
  }
  rejectLine(saleUid: string, lineUid: string, reason: string | null): Observable<PharmacySaleOrder> {
    return this.http.post<PharmacySaleOrder>(`${this.base}/uid/${saleUid}/lines/uid/${lineUid}/reject`, { reason });
  }
  cancelLine(saleUid: string, lineUid: string, reason: string | null): Observable<PharmacySaleOrder> {
    return this.http.post<PharmacySaleOrder>(`${this.base}/uid/${saleUid}/lines/uid/${lineUid}/cancel`, { reason });
  }

  /** Final dispense of an APPROVED line — decrements stock and marks the line SOLD. */
  dispenseLine(pharmacyUid: string, saleLineUid: string): Observable<StockMovement> {
    return this.http.post<StockMovement>(
      `${this.pharmacyBase}/pharmacies/uid/${pharmacyUid}/dispense-sale-line/uid/${saleLineUid}`, {});
  }
}
