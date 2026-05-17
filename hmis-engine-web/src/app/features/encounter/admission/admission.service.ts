import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import {
  Admission, AdmissionSearchParams, AdmissionSummary, AdmitPatientRequest, TransferWardRequest
} from './admission.types';

@Injectable({ providedIn: 'root' })
export class AdmissionService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/encounters/admissions`;

  search(params: AdmissionSearchParams = {}): Observable<PageResponse<AdmissionSummary>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.status) p = p.set('status', params.status);
    if (params.wardUid) p = p.set('wardUid', params.wardUid);
    if (params.patientUid) p = p.set('patientUid', params.patientUid);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<AdmissionSummary>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<Admission> { return this.http.get<Admission>(`${this.base}/${uid}`); }
  admit(req: AdmitPatientRequest): Observable<Admission> { return this.http.post<Admission>(this.base, req); }
  transferWard(uid: string, req: TransferWardRequest): Observable<Admission> {
    return this.http.post<Admission>(`${this.base}/${uid}/transfer`, req);
  }
  discharge(uid: string, summary: string | null): Observable<Admission> {
    return this.http.post<Admission>(`${this.base}/${uid}/discharge`, { summary });
  }
  markDeceased(uid: string, summary: string | null): Observable<Admission> {
    return this.http.post<Admission>(`${this.base}/${uid}/deceased`, { summary });
  }
  transferOut(uid: string, summary: string | null): Observable<Admission> {
    return this.http.post<Admission>(`${this.base}/${uid}/transfer-out`, { summary });
  }
  cancel(uid: string, reason: string | null): Observable<Admission> {
    return this.http.post<Admission>(`${this.base}/${uid}/cancel`, { reason });
  }
  recentForPatient(patientUid: string): Observable<AdmissionSummary[]> {
    return this.http.get<AdmissionSummary[]>(`${this.base}/by-patient/${patientUid}/recent`);
  }
}
