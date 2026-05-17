import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import {
  CreateLabTestTypeRequest, LabTestType, LabTestTypeSearchParams, UpdateLabTestTypeRequest
} from './lab-test.types';

@Injectable({ providedIn: 'root' })
export class LabTestTypeService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/masterdata/lab-tests`;

  search(params: LabTestTypeSearchParams = {}): Observable<PageResponse<LabTestType>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.active !== undefined) p = p.set('active', String(params.active));
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<LabTestType>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<LabTestType> { return this.http.get<LabTestType>(`${this.base}/uid/${uid}`); }
  create(req: CreateLabTestTypeRequest): Observable<LabTestType> { return this.http.post<LabTestType>(this.base, req); }
  update(uid: string, req: UpdateLabTestTypeRequest): Observable<LabTestType> { return this.http.put<LabTestType>(`${this.base}/uid/${uid}`, req); }
  setActive(uid: string, active: boolean): Observable<LabTestType> { return this.http.put<LabTestType>(`${this.base}/uid/${uid}/active`, { active }); }
  delete(uid: string): Observable<void> { return this.http.delete<void>(`${this.base}/uid/${uid}`); }
}
