import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { ClinicClinician } from './clinic-clinicians.types';

/**
 * The clinician ⇄ clinic affiliation: which clinicians work at a clinic. Used
 * both by the clinic admin "Clinicians" panel and by the consultation booking
 * screens to offer only the chosen clinic's clinicians.
 */
@Injectable({ providedIn: 'root' })
export class ClinicCliniciansService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/masterdata/clinics`;

  list(clinicUid: string): Observable<ClinicClinician[]> {
    return this.http.get<ClinicClinician[]>(`${this.base}/uid/${clinicUid}/clinicians`);
  }

  assign(clinicUid: string, userUid: string): Observable<ClinicClinician> {
    return this.http.post<ClinicClinician>(`${this.base}/uid/${clinicUid}/clinicians`, { userUid });
  }

  remove(clinicUid: string, userUid: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/uid/${clinicUid}/clinicians/uid/${userUid}`);
  }
}
