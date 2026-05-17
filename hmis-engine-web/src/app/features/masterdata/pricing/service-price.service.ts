import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import {
  ServicePrice, ServicePriceSearchParams, SetServicePriceRequest
} from './service-price.types';

@Injectable({ providedIn: 'root' })
export class ServicePriceService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/masterdata/service-prices`;

  search(params: ServicePriceSearchParams = {}): Observable<PageResponse<ServicePrice>> {
    let p = new HttpParams();
    if (params.planUid) p = p.set('planUid', params.planUid);
    if (params.kind) p = p.set('kind', params.kind);
    if (params.serviceUid) p = p.set('serviceUid', params.serviceUid);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<ServicePrice>>(this.base, { params: p });
  }

  setPrice(req: SetServicePriceRequest): Observable<ServicePrice> {
    return this.http.put<ServicePrice>(this.base, req);
  }

  delete(uid: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/uid/${uid}`);
  }
}
