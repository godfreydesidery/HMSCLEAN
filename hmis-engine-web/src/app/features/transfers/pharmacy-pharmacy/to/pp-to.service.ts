import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { PageResponse } from '../../../../core/http/page.types';
import {
  CreateTORequest, RoPickDetail, RoPickSummary, RoSearchParams,
  TODto, TOSearchParams, TOSummary
} from './pp-to.types';

@Injectable({ providedIn: 'root' })
export class PpTOService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/transfers/pharmacy-pharmacy/to`;
  private readonly roBase = `${environment.apiUrl}/transfers/pharmacy-pharmacy/ro`;

  /* ----- TO -------------------------------------------------------------- */

  search(params: TOSearchParams = {}): Observable<PageResponse<TOSummary>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.status) p = p.set('status', params.status);
    if (params.requestingPharmacyUid) p = p.set('requestingPharmacyUid', params.requestingPharmacyUid);
    if (params.deliveringPharmacyUid) p = p.set('deliveringPharmacyUid', params.deliveringPharmacyUid);
    if (params.roUid) p = p.set('roUid', params.roUid);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<TOSummary>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<TODto> {
    return this.http.get<TODto>(`${this.base}/uid/${uid}`);
  }

  create(req: CreateTORequest): Observable<TODto> {
    return this.http.post<TODto>(this.base, req);
  }

  verify(uid: string): Observable<TODto> {
    return this.http.post<TODto>(`${this.base}/uid/${uid}/verify`, {});
  }
  approve(uid: string): Observable<TODto> {
    return this.http.post<TODto>(`${this.base}/uid/${uid}/approve`, {});
  }
  issue(uid: string): Observable<TODto> {
    return this.http.post<TODto>(`${this.base}/uid/${uid}/issue`, {});
  }
  reject(uid: string, reason: string | null): Observable<TODto> {
    return this.http.post<TODto>(`${this.base}/uid/${uid}/reject`, { reason });
  }

  /* ----- RO read helpers (prefill chain — TO create starts from an RO) ---- */

  searchRos(params: RoSearchParams = {}): Observable<PageResponse<RoPickSummary>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.status) p = p.set('status', params.status);
    if (params.requestingPharmacyUid) p = p.set('requestingPharmacyUid', params.requestingPharmacyUid);
    if (params.deliveringPharmacyUid) p = p.set('deliveringPharmacyUid', params.deliveringPharmacyUid);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<RoPickSummary>>(this.roBase, { params: p });
  }

  getRo(uid: string): Observable<RoPickDetail> {
    return this.http.get<RoPickDetail>(`${this.roBase}/uid/${uid}`);
  }
}
