import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  CarePlanItem, CreateDressingEntryRequest, CreateVitalsEntryRequest, DressingEntry,
  SaveCarePlanItemRequest, VitalsEntry
} from './nursing-chart.types';

/**
 * Admission nursing chart — vitals observations and the problem/goal/intervention
 * care plan (NursingChartController). Dressings share the controller and can be
 * added here when that tab lands.
 */
@Injectable({ providedIn: 'root' })
export class NursingChartService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/encounters`;

  // ----- vitals -------------------------------------------------------------

  listVitals(admissionUid: string): Observable<VitalsEntry[]> {
    return this.http.get<VitalsEntry[]>(`${this.base}/admissions/uid/${admissionUid}/vitals`);
  }

  recordVitals(admissionUid: string, req: CreateVitalsEntryRequest): Observable<VitalsEntry> {
    return this.http.post<VitalsEntry>(`${this.base}/admissions/uid/${admissionUid}/vitals`, req);
  }

  // ----- care plan ----------------------------------------------------------

  listCarePlan(admissionUid: string): Observable<CarePlanItem[]> {
    return this.http.get<CarePlanItem[]>(`${this.base}/admissions/uid/${admissionUid}/care-plan`);
  }

  addCarePlanItem(admissionUid: string, req: SaveCarePlanItemRequest): Observable<CarePlanItem> {
    return this.http.post<CarePlanItem>(`${this.base}/admissions/uid/${admissionUid}/care-plan`, req);
  }

  updateCarePlanItem(itemUid: string, req: SaveCarePlanItemRequest): Observable<CarePlanItem> {
    return this.http.put<CarePlanItem>(`${this.base}/care-plan-items/uid/${itemUid}`, req);
  }

  resolveCarePlanItem(itemUid: string, evaluation: string | null): Observable<CarePlanItem> {
    return this.http.post<CarePlanItem>(`${this.base}/care-plan-items/uid/${itemUid}/resolve`, { evaluation });
  }

  cancelCarePlanItem(itemUid: string, reason: string | null): Observable<CarePlanItem> {
    return this.http.post<CarePlanItem>(`${this.base}/care-plan-items/uid/${itemUid}/cancel`, { reason });
  }

  // ----- dressing chart -----------------------------------------------------

  listDressings(admissionUid: string): Observable<DressingEntry[]> {
    return this.http.get<DressingEntry[]>(`${this.base}/admissions/uid/${admissionUid}/dressings`);
  }

  recordDressing(admissionUid: string, req: CreateDressingEntryRequest): Observable<DressingEntry> {
    return this.http.post<DressingEntry>(`${this.base}/admissions/uid/${admissionUid}/dressings`, req);
  }
}
