import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import { CreatePharmacyRequest, Pharmacy, PharmacySearchParams, UpdatePharmacyRequest } from './pharmacy.types';

@Injectable({ providedIn: 'root' })
export class PharmacyService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/masterdata/pharmacies`;

  search(params: PharmacySearchParams = {}): Observable<PageResponse<Pharmacy>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.active !== undefined) p = p.set('active', String(params.active));
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<Pharmacy>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<Pharmacy> { return this.http.get<Pharmacy>(`${this.base}/${uid}`); }
  create(req: CreatePharmacyRequest): Observable<Pharmacy> { return this.http.post<Pharmacy>(this.base, req); }
  update(uid: string, req: UpdatePharmacyRequest): Observable<Pharmacy> { return this.http.put<Pharmacy>(`${this.base}/${uid}`, req); }
  setActive(uid: string, active: boolean): Observable<Pharmacy> { return this.http.put<Pharmacy>(`${this.base}/${uid}/active`, { active }); }
  delete(uid: string): Observable<void> { return this.http.delete<void>(`${this.base}/${uid}`); }
}
