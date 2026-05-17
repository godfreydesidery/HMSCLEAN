import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { AddDiagnosisRequest, ConsultationDiagnosis } from './consultation-diagnosis.types';

@Injectable({ providedIn: 'root' })
export class ConsultationDiagnosisService {
  private readonly http = inject(HttpClient);

  private base(consultationUid: string): string {
    return `${environment.apiUrl}/encounters/consultations/${consultationUid}/diagnoses`;
  }

  list(consultationUid: string): Observable<ConsultationDiagnosis[]> {
    return this.http.get<ConsultationDiagnosis[]>(this.base(consultationUid));
  }

  add(consultationUid: string, req: AddDiagnosisRequest): Observable<ConsultationDiagnosis> {
    return this.http.post<ConsultationDiagnosis>(this.base(consultationUid), req);
  }

  remove(consultationUid: string, uid: string): Observable<void> {
    return this.http.delete<void>(`${this.base(consultationUid)}/${uid}`);
  }
}
