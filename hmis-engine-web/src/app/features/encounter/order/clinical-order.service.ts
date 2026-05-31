import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { ClinicalOrder, CreateOrderRequest } from './clinical-order.types';

@Injectable({ providedIn: 'root' })
export class ClinicalOrderService {
  private readonly http = inject(HttpClient);
  private readonly apiBase = `${environment.apiUrl}/encounters`;

  list(consultationUid: string): Observable<ClinicalOrder[]> {
    return this.http.get<ClinicalOrder[]>(`${this.apiBase}/consultations/uid/${consultationUid}/orders`);
  }

  request(consultationUid: string, req: CreateOrderRequest): Observable<ClinicalOrder> {
    return this.http.post<ClinicalOrder>(`${this.apiBase}/consultations/uid/${consultationUid}/orders`, req);
  }

  listOutsiderForPatient(patientUid: string): Observable<ClinicalOrder[]> {
    return this.http.get<ClinicalOrder[]>(`${this.apiBase}/patients/uid/${patientUid}/outsider-orders`);
  }

  requestForOutsider(patientUid: string, req: CreateOrderRequest): Observable<ClinicalOrder> {
    return this.http.post<ClinicalOrder>(`${this.apiBase}/patients/uid/${patientUid}/outsider-orders`, req);
  }

  accept(orderUid: string): Observable<ClinicalOrder> {
    return this.http.post<ClinicalOrder>(`${this.apiBase}/orders/uid/${orderUid}/accept`, {});
  }

  approve(orderUid: string): Observable<ClinicalOrder> {
    return this.http.post<ClinicalOrder>(`${this.apiBase}/orders/uid/${orderUid}/approve`, {});
  }

  start(orderUid: string): Observable<ClinicalOrder> {
    return this.http.post<ClinicalOrder>(`${this.apiBase}/orders/uid/${orderUid}/start`, {});
  }

  complete(orderUid: string, result: string | null): Observable<ClinicalOrder> {
    return this.http.post<ClinicalOrder>(`${this.apiBase}/orders/uid/${orderUid}/complete`, { result });
  }

  cancel(orderUid: string, reason: string | null): Observable<ClinicalOrder> {
    return this.http.post<ClinicalOrder>(`${this.apiBase}/orders/uid/${orderUid}/cancel`, { reason });
  }

  /** Reject a lab/radiology order with a reason (recoverable — re-accept resumes it). */
  reject(orderUid: string, reason: string): Observable<ClinicalOrder> {
    return this.http.post<ClinicalOrder>(`${this.apiBase}/orders/uid/${orderUid}/reject`, { reason });
  }

  /** Hold an accepted lab/radiology order (bounces it back to the pending queue). */
  hold(orderUid: string): Observable<ClinicalOrder> {
    return this.http.post<ClinicalOrder>(`${this.apiBase}/orders/uid/${orderUid}/hold`, {});
  }

  /** Book a theatre + time slot for a PROCEDURE order (scheduledAt is an ISO instant). */
  schedule(orderUid: string, req: { theatreUid: string; scheduledAt: string }): Observable<ClinicalOrder> {
    return this.http.post<ClinicalOrder>(`${this.apiBase}/orders/uid/${orderUid}/schedule`, req);
  }
}
