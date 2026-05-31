import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { PageResponse } from '../../../../core/http/page.types';
import { CreateReturnRequest, ReturnDto, ReturnSearchParams, ReturnSummary } from './return.types';

@Injectable({ providedIn: 'root' })
export class ReturnService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/transfers/pharmacy-store/returns`;

  search(params: ReturnSearchParams = {}): Observable<PageResponse<ReturnSummary>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.status) p = p.set('status', params.status);
    if (params.pharmacyUid) p = p.set('pharmacyUid', params.pharmacyUid);
    if (params.storeUid) p = p.set('storeUid', params.storeUid);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<ReturnSummary>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<ReturnDto> {
    return this.http.get<ReturnDto>(`${this.base}/uid/${uid}`);
  }

  create(req: CreateReturnRequest): Observable<ReturnDto> {
    return this.http.post<ReturnDto>(this.base, req);
  }

  submit(uid: string): Observable<ReturnDto> {
    return this.http.post<ReturnDto>(`${this.base}/uid/${uid}/submit`, {});
  }

  complete(uid: string): Observable<ReturnDto> {
    return this.http.post<ReturnDto>(`${this.base}/uid/${uid}/complete`, {});
  }

  reject(uid: string, reason: string | null): Observable<ReturnDto> {
    return this.http.post<ReturnDto>(`${this.base}/uid/${uid}/reject`, { reason });
  }

  cancel(uid: string): Observable<ReturnDto> {
    return this.http.post<ReturnDto>(`${this.base}/uid/${uid}/cancel`, {});
  }
}
