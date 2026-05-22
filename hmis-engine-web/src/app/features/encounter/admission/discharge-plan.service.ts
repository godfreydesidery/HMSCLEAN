import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { DischargePlan, DischargePlanRequest } from './discharge-plan.types';

@Injectable({ providedIn: 'root' })
export class DischargePlanService {
  private readonly http = inject(HttpClient);
  private base(admissionUid: string): string {
    return `${environment.apiUrl}/encounters/admissions/uid/${admissionUid}/discharge-plan`;
  }

  /** Returns the plan, or null when none exists yet (backend 404 → caller maps to null). */
  find(admissionUid: string): Observable<DischargePlan> {
    return this.http.get<DischargePlan>(this.base(admissionUid));
  }

  create(admissionUid: string, req: DischargePlanRequest): Observable<DischargePlan> {
    return this.http.post<DischargePlan>(this.base(admissionUid), req);
  }

  update(admissionUid: string, req: DischargePlanRequest): Observable<DischargePlan> {
    return this.http.put<DischargePlan>(this.base(admissionUid), req);
  }

  approve(admissionUid: string): Observable<DischargePlan> {
    return this.http.post<DischargePlan>(`${this.base(admissionUid)}/approve`, {});
  }

  cancel(admissionUid: string, reason: string | null): Observable<DischargePlan> {
    return this.http.post<DischargePlan>(`${this.base(admissionUid)}/cancel`, { reason });
  }
}
