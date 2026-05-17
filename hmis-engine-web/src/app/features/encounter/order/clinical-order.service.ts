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
    return this.http.get<ClinicalOrder[]>(`${this.apiBase}/consultations/${consultationUid}/orders`);
  }

  request(consultationUid: string, req: CreateOrderRequest): Observable<ClinicalOrder> {
    return this.http.post<ClinicalOrder>(`${this.apiBase}/consultations/${consultationUid}/orders`, req);
  }

  start(uid: string): Observable<ClinicalOrder> {
    return this.http.post<ClinicalOrder>(`${this.apiBase}/orders/${uid}/start`, {});
  }

  complete(uid: string, result: string | null): Observable<ClinicalOrder> {
    return this.http.post<ClinicalOrder>(`${this.apiBase}/orders/${uid}/complete`, { result });
  }

  cancel(uid: string, reason: string | null): Observable<ClinicalOrder> {
    return this.http.post<ClinicalOrder>(`${this.apiBase}/orders/${uid}/cancel`, { reason });
  }
}
