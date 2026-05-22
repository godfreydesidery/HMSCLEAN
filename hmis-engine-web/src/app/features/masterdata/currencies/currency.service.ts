import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import { CreateCurrencyRequest, Currency, CurrencySearchParams, UpdateCurrencyRequest } from './currency.types';

@Injectable({ providedIn: 'root' })
export class CurrencyService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/masterdata/currencies`;

  search(params: CurrencySearchParams = {}): Observable<PageResponse<Currency>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.active !== undefined) p = p.set('active', String(params.active));
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<Currency>>(this.base, { params: p });
  }

  findDefault(): Observable<Currency> { return this.http.get<Currency>(`${this.base}/default`); }
  findByUid(uid: string): Observable<Currency> { return this.http.get<Currency>(`${this.base}/uid/${uid}`); }
  create(req: CreateCurrencyRequest): Observable<Currency> { return this.http.post<Currency>(this.base, req); }
  update(uid: string, req: UpdateCurrencyRequest): Observable<Currency> { return this.http.put<Currency>(`${this.base}/uid/${uid}`, req); }
  setActive(uid: string, active: boolean): Observable<Currency> { return this.http.put<Currency>(`${this.base}/uid/${uid}/active`, { active }); }
  setDefault(uid: string): Observable<Currency> { return this.http.post<Currency>(`${this.base}/uid/${uid}/default`, {}); }
  delete(uid: string): Observable<void> { return this.http.delete<void>(`${this.base}/uid/${uid}`); }
}
