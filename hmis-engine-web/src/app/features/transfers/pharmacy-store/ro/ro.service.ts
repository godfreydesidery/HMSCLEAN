import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { PageResponse } from '../../../../core/http/page.types';
import { CreateRORequest, RODto, ROSearchParams, ROSummary } from './ro.types';

@Injectable({ providedIn: 'root' })
export class RoService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/transfers/pharmacy-store/ro`;

  search(params: ROSearchParams = {}): Observable<PageResponse<ROSummary>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.status) p = p.set('status', params.status);
    if (params.pharmacyUid) p = p.set('pharmacyUid', params.pharmacyUid);
    if (params.storeUid) p = p.set('storeUid', params.storeUid);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<ROSummary>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<RODto> {
    return this.http.get<RODto>(`${this.base}/uid/${uid}`);
  }

  create(req: CreateRORequest): Observable<RODto> {
    return this.http.post<RODto>(this.base, req);
  }

  verify(uid: string): Observable<RODto> {
    return this.http.post<RODto>(`${this.base}/uid/${uid}/verify`, {});
  }

  approve(uid: string): Observable<RODto> {
    return this.http.post<RODto>(`${this.base}/uid/${uid}/approve`, {});
  }

  submit(uid: string): Observable<RODto> {
    return this.http.post<RODto>(`${this.base}/uid/${uid}/submit`, {});
  }

  reject(uid: string, reason: string | null): Observable<RODto> {
    return this.http.post<RODto>(`${this.base}/uid/${uid}/reject`, { reason });
  }

  return(uid: string, reason: string | null): Observable<RODto> {
    return this.http.post<RODto>(`${this.base}/uid/${uid}/return`, { reason });
  }
}
