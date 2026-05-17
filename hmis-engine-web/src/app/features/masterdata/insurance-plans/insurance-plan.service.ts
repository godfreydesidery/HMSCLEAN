import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import {
  CreateInsurancePlanRequest, InsurancePlan, InsurancePlanSearchParams, UpdateInsurancePlanRequest
} from './insurance-plan.types';

@Injectable({ providedIn: 'root' })
export class InsurancePlanService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/masterdata/insurance-plans`;

  search(params: InsurancePlanSearchParams = {}): Observable<PageResponse<InsurancePlan>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.active !== undefined) p = p.set('active', String(params.active));
    if (params.providerUid) p = p.set('providerUid', params.providerUid);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<InsurancePlan>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<InsurancePlan> { return this.http.get<InsurancePlan>(`${this.base}/${uid}`); }
  create(req: CreateInsurancePlanRequest): Observable<InsurancePlan> { return this.http.post<InsurancePlan>(this.base, req); }
  update(uid: string, req: UpdateInsurancePlanRequest): Observable<InsurancePlan> { return this.http.put<InsurancePlan>(`${this.base}/${uid}`, req); }
  setActive(uid: string, active: boolean): Observable<InsurancePlan> { return this.http.put<InsurancePlan>(`${this.base}/${uid}/active`, { active }); }
  delete(uid: string): Observable<void> { return this.http.delete<void>(`${this.base}/${uid}`); }
}
