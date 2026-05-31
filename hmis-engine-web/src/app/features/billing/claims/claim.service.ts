import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import {
  AssembleClaimRequest, Claim, ClaimSearchParams, ClaimSummary,
  RecordSettlementRequest, RejectClaimRequest
} from './claim.types';

@Injectable({ providedIn: 'root' })
export class ClaimService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/billing/claims`;

  search(params: ClaimSearchParams = {}): Observable<PageResponse<ClaimSummary>> {
    let p = new HttpParams();
    if (params.status) p = p.set('status', params.status);
    if (params.payerPlanUid) p = p.set('payerPlanUid', params.payerPlanUid);
    if (params.providerUid) p = p.set('providerUid', params.providerUid);
    if (params.membershipNo) p = p.set('membershipNo', params.membershipNo);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<ClaimSummary>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<Claim> {
    return this.http.get<Claim>(`${this.base}/uid/${uid}`);
  }

  assemble(req: AssembleClaimRequest): Observable<Claim> {
    return this.http.post<Claim>(this.base, req);
  }

  submit(uid: string): Observable<Claim> {
    return this.http.post<Claim>(`${this.base}/uid/${uid}/submit`, {});
  }

  recordSettlement(uid: string, req: RecordSettlementRequest): Observable<Claim> {
    return this.http.post<Claim>(`${this.base}/uid/${uid}/settlements`, req);
  }

  reject(uid: string, req: RejectClaimRequest): Observable<Claim> {
    return this.http.post<Claim>(`${this.base}/uid/${uid}/reject`, req);
  }

  discard(uid: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/uid/${uid}`);
  }
}
