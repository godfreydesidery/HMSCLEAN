import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import { CreateTheatreRequest, Theatre, TheatreSearchParams, UpdateTheatreRequest } from './theatre.types';

@Injectable({ providedIn: 'root' })
export class TheatreService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/masterdata/theatres`;

  search(params: TheatreSearchParams = {}): Observable<PageResponse<Theatre>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.active !== undefined) p = p.set('active', String(params.active));
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<Theatre>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<Theatre> {
    return this.http.get<Theatre>(`${this.base}/uid/${uid}`);
  }

  create(req: CreateTheatreRequest): Observable<Theatre> {
    return this.http.post<Theatre>(this.base, req);
  }

  update(uid: string, req: UpdateTheatreRequest): Observable<Theatre> {
    return this.http.put<Theatre>(`${this.base}/uid/${uid}`, req);
  }

  setActive(uid: string, active: boolean): Observable<Theatre> {
    return this.http.put<Theatre>(`${this.base}/uid/${uid}/active`, { active });
  }

  delete(uid: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/uid/${uid}`);
  }
}
