import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import {
  CreateSupplierRequest, Supplier, SupplierSearchParams, UpdateSupplierRequest
} from './supplier.types';

@Injectable({ providedIn: 'root' })
export class SupplierService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/procurement/suppliers`;

  search(params: SupplierSearchParams = {}): Observable<PageResponse<Supplier>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.active !== undefined) p = p.set('active', String(params.active));
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<Supplier>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<Supplier> { return this.http.get<Supplier>(`${this.base}/${uid}`); }
  create(req: CreateSupplierRequest): Observable<Supplier> { return this.http.post<Supplier>(this.base, req); }
  update(uid: string, req: UpdateSupplierRequest): Observable<Supplier> { return this.http.put<Supplier>(`${this.base}/${uid}`, req); }
  setActive(uid: string, active: boolean): Observable<Supplier> { return this.http.put<Supplier>(`${this.base}/${uid}/active`, { active }); }
}
