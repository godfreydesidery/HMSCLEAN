import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PageResponse } from '../../core/http/page.types';
import { OrderWorklistParams, OrderWorklistRow } from './order-worklist.types';

@Injectable({ providedIn: 'root' })
export class OrderWorklistService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/encounters/orders`;

  search(params: OrderWorklistParams = {}): Observable<PageResponse<OrderWorklistRow>> {
    let p = new HttpParams();
    if (params.kind) p = p.set('kind', params.kind);
    if (params.status) p = p.set('status', params.status);
    if (params.patientClass) p = p.set('patientClass', params.patientClass);
    if (params.settledOnly !== undefined) p = p.set('settledOnly', String(params.settledOnly));
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<OrderWorklistRow>>(this.base, { params: p });
  }
}
