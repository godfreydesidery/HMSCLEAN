import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import {
  CreateInsuranceProviderRequest, InsuranceProvider, InsuranceProviderSearchParams, UpdateInsuranceProviderRequest
} from './insurance.types';

@Injectable({ providedIn: 'root' })
export class InsuranceProviderService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/masterdata/insurance-providers`;

  search(params: InsuranceProviderSearchParams = {}): Observable<PageResponse<InsuranceProvider>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.active !== undefined) p = p.set('active', String(params.active));
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<InsuranceProvider>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<InsuranceProvider> { return this.http.get<InsuranceProvider>(`${this.base}/uid/${uid}`); }
  create(req: CreateInsuranceProviderRequest): Observable<InsuranceProvider> { return this.http.post<InsuranceProvider>(this.base, req); }
  update(uid: string, req: UpdateInsuranceProviderRequest): Observable<InsuranceProvider> { return this.http.put<InsuranceProvider>(`${this.base}/uid/${uid}`, req); }
  setActive(uid: string, active: boolean): Observable<InsuranceProvider> { return this.http.put<InsuranceProvider>(`${this.base}/uid/${uid}/active`, { active }); }
  delete(uid: string): Observable<void> { return this.http.delete<void>(`${this.base}/uid/${uid}`); }
}
