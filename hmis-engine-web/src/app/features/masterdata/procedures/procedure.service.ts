import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import {
  CreateProcedureTypeRequest, ProcedureType, ProcedureTypeSearchParams, UpdateProcedureTypeRequest
} from './procedure.types';

@Injectable({ providedIn: 'root' })
export class ProcedureTypeService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/masterdata/procedures`;

  search(params: ProcedureTypeSearchParams = {}): Observable<PageResponse<ProcedureType>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.active !== undefined) p = p.set('active', String(params.active));
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<ProcedureType>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<ProcedureType> { return this.http.get<ProcedureType>(`${this.base}/uid/${uid}`); }
  create(req: CreateProcedureTypeRequest): Observable<ProcedureType> { return this.http.post<ProcedureType>(this.base, req); }
  update(uid: string, req: UpdateProcedureTypeRequest): Observable<ProcedureType> { return this.http.put<ProcedureType>(`${this.base}/uid/${uid}`, req); }
  setActive(uid: string, active: boolean): Observable<ProcedureType> { return this.http.put<ProcedureType>(`${this.base}/uid/${uid}/active`, { active }); }
  delete(uid: string): Observable<void> { return this.http.delete<void>(`${this.base}/uid/${uid}`); }
}
