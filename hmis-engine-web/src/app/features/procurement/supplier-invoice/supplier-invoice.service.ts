import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import {
  CreateSupplierInvoiceRequest, MarkPaidRequest, SupplierInvoice, SupplierInvoiceSearchParams,
  SupplierInvoiceSummary
} from './supplier-invoice.types';

@Injectable({ providedIn: 'root' })
export class SupplierInvoiceService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/procurement/supplier-invoices`;

  search(params: SupplierInvoiceSearchParams = {}): Observable<PageResponse<SupplierInvoiceSummary>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.status) p = p.set('status', params.status);
    if (params.supplierUid) p = p.set('supplierUid', params.supplierUid);
    if (params.orderUid) p = p.set('orderUid', params.orderUid);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<SupplierInvoiceSummary>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<SupplierInvoice> { return this.http.get<SupplierInvoice>(`${this.base}/uid/${uid}`); }
  create(req: CreateSupplierInvoiceRequest): Observable<SupplierInvoice> { return this.http.post<SupplierInvoice>(this.base, req); }

  // lifecycle: DRAFT -> SUBMITTED -> APPROVED (3-way match) -> PAID; DRAFT->CANCELLED; SUBMITTED->REJECTED
  submit(uid: string): Observable<SupplierInvoice> { return this.http.post<SupplierInvoice>(`${this.base}/uid/${uid}/submit`, {}); }
  approve(uid: string): Observable<SupplierInvoice> { return this.http.post<SupplierInvoice>(`${this.base}/uid/${uid}/approve`, {}); }
  pay(uid: string, req: MarkPaidRequest): Observable<SupplierInvoice> { return this.http.post<SupplierInvoice>(`${this.base}/uid/${uid}/pay`, req); }
  reject(uid: string, reason: string | null): Observable<SupplierInvoice> { return this.http.post<SupplierInvoice>(`${this.base}/uid/${uid}/reject`, { reason }); }
  cancel(uid: string): Observable<SupplierInvoice> { return this.http.post<SupplierInvoice>(`${this.base}/uid/${uid}/cancel`, {}); }
}
