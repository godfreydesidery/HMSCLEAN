import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { PageResponse } from '../../../../core/http/page.types';
import {
  CreateRNRequest, RNDto, RNSearchParams, RNSummary,
  ToPickDetail, ToPickSearchParams, ToPickSummary
} from './rn.types';

@Injectable({ providedIn: 'root' })
export class RnService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/transfers/pharmacy-store/rn`;
  private readonly toBase = `${environment.apiUrl}/transfers/pharmacy-store/to`;

  // ----- RN ----------------------------------------------------------------

  search(params: RNSearchParams = {}): Observable<PageResponse<RNSummary>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.status) p = p.set('status', params.status);
    if (params.pharmacyUid) p = p.set('pharmacyUid', params.pharmacyUid);
    if (params.storeUid) p = p.set('storeUid', params.storeUid);
    if (params.toUid) p = p.set('toUid', params.toUid);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<RNSummary>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<RNDto> {
    return this.http.get<RNDto>(`${this.base}/uid/${uid}`);
  }

  create(req: CreateRNRequest): Observable<RNDto> {
    return this.http.post<RNDto>(this.base, req);
  }

  // ----- TO read helpers (prefill source — self-contained) -----------------

  /** Search issued TOs (status=GOODS_ISSUED) to start a receive note from. */
  searchTos(params: ToPickSearchParams = {}): Observable<PageResponse<ToPickSummary>> {
    let p = new HttpParams().set('status', 'GOODS_ISSUED');
    if (params.query) p = p.set('query', params.query);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<ToPickSummary>>(this.toBase, { params: p });
  }

  /** Load a single TO (with lines) to prefill RN lines. */
  getTo(uid: string): Observable<ToPickDetail> {
    return this.http.get<ToPickDetail>(`${this.toBase}/uid/${uid}`);
  }
}
