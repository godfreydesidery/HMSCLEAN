import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import {
  AdjustStockRequest, ReceiveStockRequest, StockBalance, StockMovement, StockMovementKind
} from './stock.types';

export interface MovementSearchParams {
  pharmacyUid?: string;
  medicineUid?: string;
  kind?: StockMovementKind;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class StockService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/pharmacy`;

  listBalances(pharmacyUid: string): Observable<StockBalance[]> {
    return this.http.get<StockBalance[]>(`${this.base}/pharmacies/${pharmacyUid}/stock`);
  }

  receive(pharmacyUid: string, req: ReceiveStockRequest): Observable<StockBalance> {
    return this.http.post<StockBalance>(`${this.base}/pharmacies/${pharmacyUid}/stock/receive`, req);
  }

  adjust(pharmacyUid: string, req: AdjustStockRequest): Observable<StockBalance> {
    return this.http.post<StockBalance>(`${this.base}/pharmacies/${pharmacyUid}/stock/adjust`, req);
  }

  dispense(pharmacyUid: string, prescriptionUid: string): Observable<StockMovement> {
    return this.http.post<StockMovement>(`${this.base}/pharmacies/${pharmacyUid}/dispense/${prescriptionUid}`, {});
  }

  searchMovements(params: MovementSearchParams = {}): Observable<PageResponse<StockMovement>> {
    let p = new HttpParams();
    if (params.pharmacyUid) p = p.set('pharmacyUid', params.pharmacyUid);
    if (params.medicineUid) p = p.set('medicineUid', params.medicineUid);
    if (params.kind) p = p.set('kind', params.kind);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<StockMovement>>(`${this.base}/stock/movements`, { params: p });
  }
}
