import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import {
  AddLineRequest, CreatePurchaseOrderRequest, GoodsReceipt, PurchaseOrder,
  PurchaseOrderSearchParams, PurchaseOrderSummary, RecordReceiptRequest, UpdateLineRequest
} from './purchase-order.types';

@Injectable({ providedIn: 'root' })
export class PurchaseOrderService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/procurement/purchase-orders`;

  search(params: PurchaseOrderSearchParams = {}): Observable<PageResponse<PurchaseOrderSummary>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.status) p = p.set('status', params.status);
    if (params.supplierUid) p = p.set('supplierUid', params.supplierUid);
    if (params.pharmacyUid) p = p.set('pharmacyUid', params.pharmacyUid);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<PurchaseOrderSummary>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<PurchaseOrder> { return this.http.get<PurchaseOrder>(`${this.base}/uid/${uid}`); }
  create(req: CreatePurchaseOrderRequest): Observable<PurchaseOrder> { return this.http.post<PurchaseOrder>(this.base, req); }

  addLine(uid: string, req: AddLineRequest): Observable<PurchaseOrder> {
    return this.http.post<PurchaseOrder>(`${this.base}/uid/${uid}/lines`, req);
  }
  updateLine(uid: string, lineUid: string, req: UpdateLineRequest): Observable<PurchaseOrder> {
    return this.http.put<PurchaseOrder>(`${this.base}/uid/${uid}/lines/uid/${lineUid}`, req);
  }
  removeLine(uid: string, lineUid: string): Observable<PurchaseOrder> {
    return this.http.delete<PurchaseOrder>(`${this.base}/uid/${uid}/lines/uid/${lineUid}`);
  }

  markOrdered(uid: string): Observable<PurchaseOrder> {
    return this.http.post<PurchaseOrder>(`${this.base}/uid/${uid}/order`, {});
  }
  cancel(uid: string, reason: string | null): Observable<PurchaseOrder> {
    return this.http.post<PurchaseOrder>(`${this.base}/uid/${uid}/cancel`, { reason });
  }

  recordReceipt(uid: string, req: RecordReceiptRequest): Observable<GoodsReceipt> {
    return this.http.post<GoodsReceipt>(`${this.base}/uid/${uid}/receipts`, req);
  }
  listReceipts(uid: string): Observable<GoodsReceipt[]> {
    return this.http.get<GoodsReceipt[]>(`${this.base}/uid/${uid}/receipts`);
  }
}
