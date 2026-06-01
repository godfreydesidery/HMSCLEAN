import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import {
  AcceptTransferRequest, CancelTransferRequest, Consultation, ConsultationSearchParams,
  ConsultationSummary, ConsultationTransfer, ConsultationTransferStatus, RaiseTransferRequest,
  StartConsultationRequest
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

  /** The signed-in clinician's "from reception" queue — fee-settled BOOKED consultations. */
  receptionQueue(params: { page?: number; size?: number } = {}): Observable<PageResponse<ConsultationSummary>> {
    let p = new HttpParams();
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    return this.http.get<PageResponse<ConsultationSummary>>(`${this.base}/reception-queue`, { params: p });
  }

  /** The signed-in clinician's OWN active consultations (BOOKED / IN_PROGRESS / TRANSFERRED), newest first. */
  myOpen(params: { page?: number; size?: number } = {}): Observable<PageResponse<ConsultationSummary>> {
    let p = new HttpParams();
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    return this.http.get<PageResponse<ConsultationSummary>>(`${this.base}/my-open`, { params: p });
  }

  findByUid(uid: string): Observable<Consultation> { return this.http.get<Consultation>(`${this.base}/uid/${uid}`); }
  book(req: StartConsultationRequest): Observable<Consultation> { return this.http.post<Consultation>(this.base, req); }
  start(uid: string): Observable<Consultation> { return this.http.post<Consultation>(`${this.base}/uid/${uid}/start`, {}); }
  complete(uid: string): Observable<Consultation> { return this.http.post<Consultation>(`${this.base}/uid/${uid}/complete`, {}); }
  cancel(uid: string, reason: string | null): Observable<Consultation> { return this.http.post<Consultation>(`${this.base}/uid/${uid}/cancel`, { reason }); }

  // ----- Two-phase transfer (raise → accept / cancel) --------------------

  /**
   * Raise a transfer to a target CLINIC (no clinician). The source consultation
   * flips to TRANSFERRED; a PENDING transfer awaits reception to accept it.
   */
  raiseTransfer(consultationUid: string, req: RaiseTransferRequest): Observable<ConsultationTransfer> {
    return this.http.post<ConsultationTransfer>(`${this.base}/uid/${consultationUid}/transfer`, req);
  }

  /** Reception's incoming-transfer queue. */
  transferQueue(params: { status?: ConsultationTransferStatus; page?: number; size?: number } = {}):
    Observable<PageResponse<ConsultationTransfer>> {
    let p = new HttpParams();
    if (params.status) p = p.set('status', params.status);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    return this.http.get<PageResponse<ConsultationTransfer>>(`${this.base}/transfers`, { params: p });
  }

  /** Reception accepts a PENDING transfer, booking the receiving consultation. */
  acceptTransfer(transferUid: string, req: AcceptTransferRequest): Observable<Consultation> {
    return this.http.post<Consultation>(`${this.base}/transfers/uid/${transferUid}/accept`, req);
  }

  /** The initiating doctor cancels / reverts a still-PENDING transfer. Returns the source consultation. */
  cancelTransfer(transferUid: string, req: CancelTransferRequest): Observable<Consultation> {
    return this.http.post<Consultation>(`${this.base}/transfers/uid/${transferUid}/cancel`, req);
  }
  recentForPatient(patientUid: string): Observable<ConsultationSummary[]> {
    return this.http.get<ConsultationSummary[]>(`${this.base}/by-patient/uid/${patientUid}/recent`);
  }
}
