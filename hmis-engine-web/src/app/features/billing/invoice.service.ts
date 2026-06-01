import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { PageResponse } from '../../core/http/page.types';
import {
  AdmissionBillingSummary, CreateCreditNoteRequest, CreateRefundRequest, CreditNote, Invoice,
  InvoiceLineKind, InvoiceSearchParams, InvoiceSummary, OverrideLinePriceRequest, PayableLine,
  PayLinesRequest, PayLinesResult, RecordPaymentRequest, Refund
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

  // ----- cashier line-level payment (legacy patient-first "check to pay") ------

  /** A patient's cash-payable lines across all open invoices; `kind` segments by service till. */
  payableLines(patientUid: string, kind?: InvoiceLineKind): Observable<PayableLine[]> {
    let p = new HttpParams();
    if (kind) p = p.set('kind', kind);
    return this.http.get<PayableLine[]>(`${this.base}/patients/uid/${patientUid}/payable-lines`, { params: p });
  }

  /** Collect cash for a selected set of the patient's payable lines (legacy confirm_bills_payment). */
  payLines(patientUid: string, req: PayLinesRequest): Observable<PayLinesResult> {
    return this.http.post<PayLinesResult>(`${this.base}/patients/uid/${patientUid}/pay-lines`, req);
  }

  /** Renegotiate a line's unit price within its [min,max] band; returns the updated invoice. */
  overrideLinePrice(invoiceUid: string, lineUid: string, req: OverrideLinePriceRequest): Observable<Invoice> {
    return this.http.put<Invoice>(`${this.base}/invoices/uid/${invoiceUid}/lines/uid/${lineUid}/price`, req);
  }

  // ----- credit notes (write-downs) + refunds (return cash) --------------------
  listCreditNotes(invoiceUid: string): Observable<CreditNote[]> {
    return this.http.get<CreditNote[]>(`${this.base}/invoices/uid/${invoiceUid}/credit-notes`);
  }
  raiseCreditNote(invoiceUid: string, req: CreateCreditNoteRequest): Observable<CreditNote> {
    return this.http.post<CreditNote>(`${this.base}/invoices/uid/${invoiceUid}/credit-notes`, req);
  }
  listRefunds(invoiceUid: string): Observable<Refund[]> {
    return this.http.get<Refund[]>(`${this.base}/invoices/uid/${invoiceUid}/refunds`);
  }
  raiseRefund(invoiceUid: string, req: CreateRefundRequest): Observable<Refund> {
    return this.http.post<Refund>(`${this.base}/invoices/uid/${invoiceUid}/refunds`, req);
  }

  /** Admission discharge bill-clearance gate read (V66). 204 → null when no invoice yet. */
  admissionBillingSummary(admissionUid: string): Observable<AdmissionBillingSummary | null> {
    return this.http
      .get<AdmissionBillingSummary>(`${this.base}/admissions/uid/${admissionUid}/billing-summary`, { observe: 'response' })
      .pipe(map((res) => (res.status === 204 ? null : res.body)));
  }
}
