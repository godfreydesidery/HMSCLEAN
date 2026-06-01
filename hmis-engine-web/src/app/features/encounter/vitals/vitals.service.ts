import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import { PatientVitals, RecordVitalsRequest, VitalsWorklistRow } from './vitals.types';

@Injectable({ providedIn: 'root' })
export class VitalsService {
  private readonly http = inject(HttpClient);
  private readonly consultations = `${environment.apiUrl}/encounters/consultations`;

  private base(consultationUid: string): string {
    return `${this.consultations}/uid/${consultationUid}/vitals`;
  }

  list(consultationUid: string): Observable<PatientVitals[]> {
    return this.http.get<PatientVitals[]>(this.base(consultationUid));
  }

  /** Nurse fill / save — create-or-update the open row, leaving it PENDING. */
  record(consultationUid: string, req: RecordVitalsRequest): Observable<PatientVitals> {
    return this.http.post<PatientVitals>(this.base(consultationUid), req);
  }

  /** Nurse submit — PENDING → SUBMITTED, locks the set for the doctor. */
  submit(consultationUid: string, vitalsUid: string): Observable<PatientVitals> {
    return this.http.post<PatientVitals>(`${this.base(consultationUid)}/uid/${vitalsUid}/submit`, {});
  }

  /** Doctor consume — SUBMITTED → ARCHIVED, taken into the clinical exam. */
  consume(consultationUid: string, vitalsUid: string): Observable<PatientVitals> {
    return this.http.post<PatientVitals>(`${this.base(consultationUid)}/uid/${vitalsUid}/consume`, {});
  }

  delete(consultationUid: string, uid: string): Observable<void> {
    return this.http.delete<void>(`${this.base(consultationUid)}/uid/${uid}`);
  }

  /** The outpatient nurse-triage worklist — fee-settled BOOKED/IN_PROGRESS consultations. */
  nurseWorklist(params: { page?: number; size?: number } = {}): Observable<PageResponse<VitalsWorklistRow>> {
    let p = new HttpParams();
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    return this.http.get<PageResponse<VitalsWorklistRow>>(`${this.consultations}/nurse-worklist`, { params: p });
  }
}
