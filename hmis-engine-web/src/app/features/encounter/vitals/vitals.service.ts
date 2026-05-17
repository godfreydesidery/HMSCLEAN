import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PatientVitals, RecordVitalsRequest } from './vitals.types';

@Injectable({ providedIn: 'root' })
export class VitalsService {
  private readonly http = inject(HttpClient);

  private base(consultationUid: string): string {
    return `${environment.apiUrl}/encounters/consultations/uid/${consultationUid}/vitals`;
  }

  list(consultationUid: string): Observable<PatientVitals[]> {
    return this.http.get<PatientVitals[]>(this.base(consultationUid));
  }

  record(consultationUid: string, req: RecordVitalsRequest): Observable<PatientVitals> {
    return this.http.post<PatientVitals>(this.base(consultationUid), req);
  }

  delete(consultationUid: string, uid: string): Observable<void> {
    return this.http.delete<void>(`${this.base(consultationUid)}/uid/${uid}`);
  }
}
