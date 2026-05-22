import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import {
  CreatePayrollPeriodRequest, PayrollItem, PayrollPeriod,
  PayrollPeriodWithItems, PayrollSearchParams, UpsertPayrollItemRequest
} from './payroll.types';

@Injectable({ providedIn: 'root' })
export class PayrollService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/hr/payroll`;

  search(params: PayrollSearchParams = {}): Observable<PageResponse<PayrollPeriod>> {
    let p = new HttpParams();
    if (params.status) p = p.set('status', params.status);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort)               p = p.set('sort', params.sort);
    return this.http.get<PageResponse<PayrollPeriod>>(`${this.base}/periods`, { params: p });
  }

  findByUid(uid: string): Observable<PayrollPeriodWithItems> {
    return this.http.get<PayrollPeriodWithItems>(`${this.base}/periods/uid/${uid}`);
  }

  createPeriod(req: CreatePayrollPeriodRequest): Observable<PayrollPeriod> {
    return this.http.post<PayrollPeriod>(`${this.base}/periods`, req);
  }

  upsertItem(periodUid: string, req: UpsertPayrollItemRequest): Observable<PayrollItem> {
    return this.http.post<PayrollItem>(`${this.base}/periods/uid/${periodUid}/items`, req);
  }

  removeItem(periodUid: string, employeeUid: string): Observable<void> {
    return this.http.delete<void>(
      `${this.base}/periods/uid/${periodUid}/items/employee/uid/${employeeUid}`
    );
  }

  verify(periodUid: string): Observable<PayrollPeriod> {
    return this.http.post<PayrollPeriod>(`${this.base}/periods/uid/${periodUid}/verify`, {});
  }

  approve(periodUid: string): Observable<PayrollPeriod> {
    return this.http.post<PayrollPeriod>(`${this.base}/periods/uid/${periodUid}/approve`, {});
  }

  markPaid(periodUid: string): Observable<PayrollPeriod> {
    return this.http.post<PayrollPeriod>(`${this.base}/periods/uid/${periodUid}/pay`, {});
  }

  cancel(periodUid: string, reason: string | null): Observable<PayrollPeriod> {
    return this.http.post<PayrollPeriod>(`${this.base}/periods/uid/${periodUid}/cancel`, { reason });
  }
}
