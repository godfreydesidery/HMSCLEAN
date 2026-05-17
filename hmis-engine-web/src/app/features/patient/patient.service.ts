import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PageResponse } from '../../core/http/page.types';
import {
  CreatePatientRequest, Patient, PatientSearchParams, PatientSummary, UpdatePatientRequest
} from './patient.types';

@Injectable({ providedIn: 'root' })
export class PatientService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/patients`;

  search(params: PatientSearchParams = {}): Observable<PageResponse<PatientSummary>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.active !== undefined) p = p.set('active', String(params.active));
    if (params.gender) p = p.set('gender', params.gender);
    if (params.paymentType) p = p.set('paymentType', params.paymentType);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<PatientSummary>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<Patient> { return this.http.get<Patient>(`${this.base}/${uid}`); }
  register(req: CreatePatientRequest): Observable<Patient> { return this.http.post<Patient>(this.base, req); }
  update(uid: string, req: UpdatePatientRequest): Observable<Patient> { return this.http.put<Patient>(`${this.base}/${uid}`, req); }
  setActive(uid: string, active: boolean): Observable<Patient> { return this.http.put<Patient>(`${this.base}/${uid}/active`, { active }); }
}
