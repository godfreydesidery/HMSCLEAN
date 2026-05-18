import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import {
  Clinic,
  ClinicSearchParams,
  CreateClinicRequest,
  UpdateClinicRequest
} from './clinic.types';

@Injectable({ providedIn: 'root' })
export class ClinicService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/masterdata/clinics`;

  search(params: ClinicSearchParams = {}): Observable<PageResponse<Clinic>> {
    let httpParams = new HttpParams();
    if (params.query) {
      httpParams = httpParams.set('query', params.query);
    }
    if (params.active !== undefined) {
      httpParams = httpParams.set('active', String(params.active));
    }
    if (params.type) {
      httpParams = httpParams.set('type', params.type);
    }
    if (params.page !== undefined) {
      httpParams = httpParams.set('page', String(params.page));
    }
    if (params.size !== undefined) {
      httpParams = httpParams.set('size', String(params.size));
    }
    if (params.sort) {
      httpParams = httpParams.set('sort', params.sort);
    }
    return this.http.get<PageResponse<Clinic>>(this.base, { params: httpParams });
  }

  findByUid(uid: string): Observable<Clinic> {
    return this.http.get<Clinic>(`${this.base}/uid/${uid}`);
  }

  create(request: CreateClinicRequest): Observable<Clinic> {
    return this.http.post<Clinic>(this.base, request);
  }

  update(uid: string, request: UpdateClinicRequest): Observable<Clinic> {
    return this.http.put<Clinic>(`${this.base}/uid/${uid}`, request);
  }

  setActive(uid: string, active: boolean): Observable<Clinic> {
    return this.http.put<Clinic>(`${this.base}/uid/${uid}/active`, { active });
  }

  delete(uid: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/uid/${uid}`);
  }
}
