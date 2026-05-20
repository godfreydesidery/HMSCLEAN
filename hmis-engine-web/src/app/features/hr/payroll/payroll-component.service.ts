import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import {
  ComputePayrollRequest, ComputedPayroll, CreatePayrollComponentRequest,
  PayrollComponent, PayrollComponentSearchParams, UpdatePayrollComponentRequest
} from './payroll-component.types';

@Injectable({ providedIn: 'root' })
export class PayrollComponentService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/hr/payroll`;

  search(params: PayrollComponentSearchParams = {}): Observable<PageResponse<PayrollComponent>> {
    let p = new HttpParams();
    if (params.active !== undefined) p = p.set('active', String(params.active));
    if (params.type) p = p.set('type', params.type);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort)               p = p.set('sort', params.sort);
    return this.http.get<PageResponse<PayrollComponent>>(`${this.base}/components`, { params: p });
  }

  findByUid(uid: string): Observable<PayrollComponent> {
    return this.http.get<PayrollComponent>(`${this.base}/components/uid/${uid}`);
  }

  create(req: CreatePayrollComponentRequest): Observable<PayrollComponent> {
    return this.http.post<PayrollComponent>(`${this.base}/components`, req);
  }

  update(uid: string, req: UpdatePayrollComponentRequest): Observable<PayrollComponent> {
    return this.http.put<PayrollComponent>(`${this.base}/components/uid/${uid}`, req);
  }

  setActive(uid: string, active: boolean): Observable<PayrollComponent> {
    return this.http.put<PayrollComponent>(`${this.base}/components/uid/${uid}/active`, { active });
  }

  delete(uid: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/components/uid/${uid}`);
  }

  /** Stateless auto-prefill: compute gross/deductions from active components for a basic salary. */
  compute(req: ComputePayrollRequest): Observable<ComputedPayroll> {
    return this.http.post<ComputedPayroll>(`${this.base}/compute`, req);
  }
}
