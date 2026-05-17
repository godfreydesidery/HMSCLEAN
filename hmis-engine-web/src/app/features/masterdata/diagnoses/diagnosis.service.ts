import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import {
  CreateDiagnosisTypeRequest, DiagnosisType, DiagnosisTypeSearchParams, UpdateDiagnosisTypeRequest
} from './diagnosis.types';

@Injectable({ providedIn: 'root' })
export class DiagnosisTypeService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/masterdata/diagnoses`;

  search(params: DiagnosisTypeSearchParams = {}): Observable<PageResponse<DiagnosisType>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.active !== undefined) p = p.set('active', String(params.active));
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<DiagnosisType>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<DiagnosisType> { return this.http.get<DiagnosisType>(`${this.base}/${uid}`); }
  create(req: CreateDiagnosisTypeRequest): Observable<DiagnosisType> { return this.http.post<DiagnosisType>(this.base, req); }
  update(uid: string, req: UpdateDiagnosisTypeRequest): Observable<DiagnosisType> { return this.http.put<DiagnosisType>(`${this.base}/${uid}`, req); }
  setActive(uid: string, active: boolean): Observable<DiagnosisType> { return this.http.put<DiagnosisType>(`${this.base}/${uid}/active`, { active }); }
  delete(uid: string): Observable<void> { return this.http.delete<void>(`${this.base}/${uid}`); }
}
