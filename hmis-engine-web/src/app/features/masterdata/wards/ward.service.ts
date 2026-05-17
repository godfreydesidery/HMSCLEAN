import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import { CreateWardRequest, UpdateWardRequest, Ward, WardSearchParams } from './ward.types';

@Injectable({ providedIn: 'root' })
export class WardService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/masterdata/wards`;

  search(params: WardSearchParams = {}): Observable<PageResponse<Ward>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.active !== undefined) p = p.set('active', String(params.active));
    if (params.category) p = p.set('category', params.category);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<Ward>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<Ward> { return this.http.get<Ward>(`${this.base}/${uid}`); }
  create(req: CreateWardRequest): Observable<Ward> { return this.http.post<Ward>(this.base, req); }
  update(uid: string, req: UpdateWardRequest): Observable<Ward> { return this.http.put<Ward>(`${this.base}/${uid}`, req); }
  setActive(uid: string, active: boolean): Observable<Ward> { return this.http.put<Ward>(`${this.base}/${uid}/active`, { active }); }
  delete(uid: string): Observable<void> { return this.http.delete<void>(`${this.base}/${uid}`); }
}
