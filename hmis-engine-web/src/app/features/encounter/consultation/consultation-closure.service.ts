import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { ClosurePlan, CreateClosurePlanRequest, UpdateClosurePlanRequest } from './consultation-closure.types';

@Injectable({ providedIn: 'root' })
export class ConsultationClosureService {
  private readonly http = inject(HttpClient);
  private base(consultationUid: string): string {
    return `${environment.apiUrl}/encounters/consultations/uid/${consultationUid}/closure`;
  }

  /** Returns the plan; backend 404 when none exists yet — caller maps to "no closure plan". */
  find(consultationUid: string): Observable<ClosurePlan> {
    return this.http.get<ClosurePlan>(this.base(consultationUid));
  }

  create(consultationUid: string, req: CreateClosurePlanRequest): Observable<ClosurePlan> {
    return this.http.post<ClosurePlan>(this.base(consultationUid), req);
  }

  update(consultationUid: string, req: UpdateClosurePlanRequest): Observable<ClosurePlan> {
    return this.http.put<ClosurePlan>(this.base(consultationUid), req);
  }

  approve(consultationUid: string): Observable<ClosurePlan> {
    return this.http.post<ClosurePlan>(`${this.base(consultationUid)}/approve`, {});
  }

  cancel(consultationUid: string, reason: string | null): Observable<ClosurePlan> {
    return this.http.post<ClosurePlan>(`${this.base(consultationUid)}/cancel`, { reason });
  }
}
