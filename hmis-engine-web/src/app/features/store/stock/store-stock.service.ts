import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import {
  AdjustStoreStockRequest, ReceiveStoreStockRequest,
  StoreStockBalance, StoreStockBatch, StoreStockMovement, StoreStockMovementKind
} from './store-stock.types';

export interface StoreMovementSearchParams {
  storeUid?: string;
  medicineUid?: string;
  kind?: StoreStockMovementKind;
  page?: number;
  size?: number;
  sort?: string;
}

export interface StoreBalanceSearchParams {
  query?: string;
  lowOnly?: boolean;
  expiringOnly?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class StoreStockService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/store`;

  /** Server-side, paginated balances with name/code search + low/expiring filters. */
  searchBalances(storeUid: string, params: StoreBalanceSearchParams = {}): Observable<PageResponse<StoreStockBalance>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.lowOnly) p = p.set('lowOnly', 'true');
    if (params.expiringOnly) p = p.set('expiringOnly', 'true');
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<StoreStockBalance>>(`${this.base}/stores/uid/${storeUid}/stock`, { params: p });
  }

  receive(storeUid: string, req: ReceiveStoreStockRequest): Observable<StoreStockBatch> {
    return this.http.post<StoreStockBatch>(`${this.base}/stores/uid/${storeUid}/stock/receive`, req);
  }

  adjust(storeUid: string, req: AdjustStoreStockRequest): Observable<StoreStockBatch> {
    return this.http.post<StoreStockBatch>(`${this.base}/stores/uid/${storeUid}/stock/adjust`, req);
  }

  searchMovements(params: StoreMovementSearchParams = {}): Observable<PageResponse<StoreStockMovement>> {
    let p = new HttpParams();
    if (params.storeUid) p = p.set('storeUid', params.storeUid);
    if (params.medicineUid) p = p.set('medicineUid', params.medicineUid);
    if (params.kind) p = p.set('kind', params.kind);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<StoreStockMovement>>(`${this.base}/stock/movements`, { params: p });
  }
}
