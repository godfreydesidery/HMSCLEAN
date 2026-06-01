import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import {
  CreateDosageRequest, Dosage, DosageSearchParams, UpdateDosageRequest
} from './dosage.types';

@Injectable({ providedIn: 'root' })
export class DosageService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/masterdata/dosages`;

  search(params: DosageSearchParams = {}): Observable<PageResponse<Dosage>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.active !== undefined) p = p.set('active', String(params.active));
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<Dosage>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<Dosage> { return this.http.get<Dosage>(`${this.base}/uid/${uid}`); }
  create(req: CreateDosageRequest): Observable<Dosage> { return this.http.post<Dosage>(this.base, req); }
  update(uid: string, req: UpdateDosageRequest): Observable<Dosage> { return this.http.put<Dosage>(`${this.base}/uid/${uid}`, req); }
  setActive(uid: string, active: boolean): Observable<Dosage> { return this.http.put<Dosage>(`${this.base}/uid/${uid}/active`, { active }); }
  delete(uid: string): Observable<void> { return this.http.delete<void>(`${this.base}/uid/${uid}`); }
}
