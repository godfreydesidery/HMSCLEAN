import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { PageResponse } from '../../core/http/page.types';
import {
  Invoice, InvoiceSearchParams, InvoiceSummary, RecordPaymentRequest
} from './invoice.types';

@Injectable({ providedIn: 'root' })
export class InvoiceService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/billing`;

  search(params: InvoiceSearchParams = {}): Observable<PageResponse<InvoiceSummary>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.status) p = p.set('status', params.status);
    if (params.patientUid) p = p.set('patientUid', params.patientUid);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<InvoiceSummary>>(`${this.base}/invoices`, { params: p });
  }

  findByUid(uid: string): Observable<Invoice> {
    return this.http.get<Invoice>(`${this.base}/invoices/uid/${uid}`);
  }

  findForConsultation(consultationUid: string): Observable<Invoice | null> {
    return this.http
      .get<Invoice>(`${this.base}/consultations/uid/${consultationUid}/invoice`, { observe: 'response' })
      .pipe(map((res) => (res.status === 204 ? null : res.body)));
  }

  generateForConsultation(consultationUid: string): Observable<Invoice> {
    return this.http.post<Invoice>(`${this.base}/consultations/uid/${consultationUid}/invoice`, {});
  }

  findForAdmission(admissionUid: string): Observable<Invoice | null> {
    return this.http
      .get<Invoice>(`${this.base}/admissions/uid/${admissionUid}/invoice`, { observe: 'response' })
      .pipe(map((res) => (res.status === 204 ? null : res.body)));
  }

  generateForAdmission(admissionUid: string): Observable<Invoice> {
    return this.http.post<Invoice>(`${this.base}/admissions/uid/${admissionUid}/invoice`, {});
  }

  findCurrentOutsiderDraft(patientUid: string): Observable<Invoice | null> {
    return this.http
      .get<Invoice>(`${this.base}/patients/uid/${patientUid}/outsider-invoice`, { observe: 'response' })
      .pipe(map((res) => (res.status === 204 ? null : res.body)));
  }

  generateForOutsider(patientUid: string): Observable<Invoice> {
    return this.http.post<Invoice>(`${this.base}/patients/uid/${patientUid}/outsider-invoice`, {});
  }

  /** Read the patient's registration-fee invoice (Phase 36). 204 → null. */
  findRegistrationFee(patientUid: string): Observable<Invoice | null> {
    return this.http
      .get<Invoice>(`${this.base}/patients/uid/${patientUid}/registration-fee`, { observe: 'response' })
      .pipe(map((res) => (res.status === 204 ? null : res.body)));
  }

  /** Idempotent re-seed of the registration-fee invoice — recovery if the after-commit listener missed. */
  ensureRegistrationFee(patientUid: string): Observable<Invoice> {
    return this.http.post<Invoice>(`${this.base}/patients/uid/${patientUid}/registration-fee`, {});
  }

  issue(uid: string): Observable<Invoice> {
    return this.http.post<Invoice>(`${this.base}/invoices/uid/${uid}/issue`, {});
  }

  cancel(uid: string, reason: string | null): Observable<Invoice> {
    return this.http.post<Invoice>(`${this.base}/invoices/uid/${uid}/cancel`, { reason });
  }

  recordPayment(uid: string, req: RecordPaymentRequest): Observable<Invoice> {
    return this.http.post<Invoice>(`${this.base}/invoices/uid/${uid}/payments`, req);
  }
}
