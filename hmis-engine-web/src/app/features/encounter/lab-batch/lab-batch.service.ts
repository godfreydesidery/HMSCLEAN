import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import {
  CancelLabBatchRequest, CreateLabBatchRequest, LabBatch, LabBatchSearchParams
} from './lab-batch.types';

@Injectable({ providedIn: 'root' })
export class LabBatchService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/encounters/lab-batches`;

  search(params: LabBatchSearchParams = {}): Observable<PageResponse<LabBatch>> {
    let p = new HttpParams();
    if (params.status)         p = p.set('status', params.status);
    if (params.labTestTypeUid) p = p.set('labTestTypeUid', params.labTestTypeUid);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort)               p = p.set('sort', params.sort);
    return this.http.get<PageResponse<LabBatch>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<LabBatch> {
    return this.http.get<LabBatch>(`${this.base}/uid/${uid}`);
  }

  create(req: CreateLabBatchRequest): Observable<LabBatch> {
    return this.http.post<LabBatch>(this.base, req);
  }

  addOrder(batchUid: string, orderUid: string): Observable<LabBatch> {
    return this.http.post<LabBatch>(`${this.base}/uid/${batchUid}/orders`, { orderUid });
  }

  removeOrder(batchUid: string, orderUid: string): Observable<LabBatch> {
    return this.http.delete<LabBatch>(`${this.base}/uid/${batchUid}/orders/uid/${orderUid}`);
  }

  markProcessing(batchUid: string): Observable<LabBatch> {
    return this.http.post<LabBatch>(`${this.base}/uid/${batchUid}/process`, {});
  }

  markCompleted(batchUid: string): Observable<LabBatch> {
    return this.http.post<LabBatch>(`${this.base}/uid/${batchUid}/complete`, {});
  }

  cancel(batchUid: string, req: CancelLabBatchRequest): Observable<LabBatch> {
    return this.http.post<LabBatch>(`${this.base}/uid/${batchUid}/cancel`, req);
  }
}
