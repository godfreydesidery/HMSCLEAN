import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { CreatePrescriptionRequest, Prescription } from './prescription.types';

@Injectable({ providedIn: 'root' })
export class PrescriptionService {
  private readonly http = inject(HttpClient);
  private readonly apiBase = `${environment.apiUrl}/encounters`;

  list(consultationUid: string): Observable<Prescription[]> {
    return this.http.get<Prescription[]>(`${this.apiBase}/consultations/uid/${consultationUid}/prescriptions`);
  }

  prescribe(consultationUid: string, req: CreatePrescriptionRequest): Observable<Prescription> {
    return this.http.post<Prescription>(`${this.apiBase}/consultations/uid/${consultationUid}/prescriptions`, req);
  }

  listOutsiderForPatient(patientUid: string): Observable<Prescription[]> {
    return this.http.get<Prescription[]>(`${this.apiBase}/patients/uid/${patientUid}/outsider-prescriptions`);
  }

  prescribeForOutsider(patientUid: string, req: CreatePrescriptionRequest): Observable<Prescription> {
    return this.http.post<Prescription>(`${this.apiBase}/patients/uid/${patientUid}/outsider-prescriptions`, req);
  }

  // ---- pharmacy lifecycle transitions
  accept(prescriptionUid: string): Observable<Prescription> {
    return this.http.post<Prescription>(`${this.apiBase}/prescriptions/uid/${prescriptionUid}/accept`, {});
  }
  hold(prescriptionUid: string): Observable<Prescription> {
    return this.http.post<Prescription>(`${this.apiBase}/prescriptions/uid/${prescriptionUid}/hold`, {});
  }
  verify(prescriptionUid: string): Observable<Prescription> {
    return this.http.post<Prescription>(`${this.apiBase}/prescriptions/uid/${prescriptionUid}/verify`, {});
  }
  approve(prescriptionUid: string): Observable<Prescription> {
    return this.http.post<Prescription>(`${this.apiBase}/prescriptions/uid/${prescriptionUid}/approve`, {});
  }
  reject(prescriptionUid: string, reason: string | null): Observable<Prescription> {
    return this.http.post<Prescription>(`${this.apiBase}/prescriptions/uid/${prescriptionUid}/reject`, { reason });
  }

  cancel(prescriptionUid: string, reason: string | null): Observable<Prescription> {
    return this.http.post<Prescription>(`${this.apiBase}/prescriptions/uid/${prescriptionUid}/cancel`, { reason });
  }
}
