import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { MedicationAdministration, RecordAdministrationRequest } from './medication-admin.types';

@Injectable({ providedIn: 'root' })
export class MedicationAdminService {
  private readonly http = inject(HttpClient);
  private base(admissionUid: string): string {
    return `${environment.apiUrl}/encounters/admissions/uid/${admissionUid}/medication-administrations`;
  }

  list(admissionUid: string): Observable<MedicationAdministration[]> {
    return this.http.get<MedicationAdministration[]>(this.base(admissionUid));
  }

  record(admissionUid: string, req: RecordAdministrationRequest): Observable<MedicationAdministration> {
    return this.http.post<MedicationAdministration>(this.base(admissionUid), req);
  }
}
