import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import {
  CreateDosingFrequencyRequest, DosingFrequency, DosingFrequencySearchParams, UpdateDosingFrequencyRequest
} from './dosing-frequency.types';

@Injectable({ providedIn: 'root' })
export class DosingFrequencyService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/masterdata/dosing-frequencies`;

  search(params: DosingFrequencySearchParams = {}): Observable<PageResponse<DosingFrequency>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.active !== undefined) p = p.set('active', String(params.active));
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<DosingFrequency>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<DosingFrequency> { return this.http.get<DosingFrequency>(`${this.base}/uid/${uid}`); }
  create(req: CreateDosingFrequencyRequest): Observable<DosingFrequency> { return this.http.post<DosingFrequency>(this.base, req); }
  update(uid: string, req: UpdateDosingFrequencyRequest): Observable<DosingFrequency> { return this.http.put<DosingFrequency>(`${this.base}/uid/${uid}`, req); }
  setActive(uid: string, active: boolean): Observable<DosingFrequency> { return this.http.put<DosingFrequency>(`${this.base}/uid/${uid}/active`, { active }); }
  delete(uid: string): Observable<void> { return this.http.delete<void>(`${this.base}/uid/${uid}`); }
}
