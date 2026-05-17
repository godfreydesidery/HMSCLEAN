import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import {
  Consultation, ConsultationSearchParams, ConsultationSummary, StartConsultationRequest
} from './consultation.types';

@Injectable({ providedIn: 'root' })
export class ConsultationService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/encounters/consultations`;

  search(params: ConsultationSearchParams = {}): Observable<PageResponse<ConsultationSummary>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.status) p = p.set('status', params.status);
    if (params.clinicUid) p = p.set('clinicUid', params.clinicUid);
    if (params.patientUid) p = p.set('patientUid', params.patientUid);
    if (params.clinicianUsername) p = p.set('clinicianUsername', params.clinicianUsername);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<ConsultationSummary>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<Consultation> { return this.http.get<Consultation>(`${this.base}/uid/${uid}`); }
  book(req: StartConsultationRequest): Observable<Consultation> { return this.http.post<Consultation>(this.base, req); }
  start(uid: string): Observable<Consultation> { return this.http.post<Consultation>(`${this.base}/uid/${uid}/start`, {}); }
  complete(uid: string): Observable<Consultation> { return this.http.post<Consultation>(`${this.base}/uid/${uid}/complete`, {}); }
  cancel(uid: string, reason: string | null): Observable<Consultation> { return this.http.post<Consultation>(`${this.base}/uid/${uid}/cancel`, { reason }); }
  recentForPatient(patientUid: string): Observable<ConsultationSummary[]> {
    return this.http.get<ConsultationSummary[]>(`${this.base}/by-patient/uid/${patientUid}/recent`);
  }
}
