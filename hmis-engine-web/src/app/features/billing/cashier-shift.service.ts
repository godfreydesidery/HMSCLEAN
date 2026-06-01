import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PageResponse } from '../../core/http/page.types';
import {
  CashierShift, CashierShiftSearchParams, CloseShiftRequest, OpenShiftRequest
} from './cashier-shift.types';

@Injectable({ providedIn: 'root' })
export class CashierShiftService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/billing/cashier-shifts`;

  /** The caller's currently OPEN shift, or null when none is open (/me 404s). */
  currentOpen(): Observable<CashierShift | null> {
    return this.http.get<CashierShift>(`${this.base}/me`).pipe(
      catchError((err) => (err?.status === 404 ? of(null) : throwError(() => err)))
    );
  }

  open(req: OpenShiftRequest): Observable<CashierShift> {
    return this.http.post<CashierShift>(`${this.base}/open`, req);
  }

  close(req: CloseShiftRequest): Observable<CashierShift> {
    return this.http.post<CashierShift>(`${this.base}/close`, req);
  }

  findByUid(uid: string): Observable<CashierShift> {
    return this.http.get<CashierShift>(`${this.base}/uid/${uid}`);
  }

  search(params: CashierShiftSearchParams = {}): Observable<PageResponse<CashierShift>> {
    let p = new HttpParams();
    if (params.username) p = p.set('username', params.username);
    if (params.status) p = p.set('status', params.status);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<CashierShift>>(this.base, { params: p });
  }
}
