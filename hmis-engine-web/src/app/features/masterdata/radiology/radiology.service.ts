import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import {
  CreateRadiologyTypeRequest, RadiologyType, RadiologyTypeSearchParams, UpdateRadiologyTypeRequest
} from './radiology.types';

@Injectable({ providedIn: 'root' })
export class RadiologyTypeService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/masterdata/radiology`;

  search(params: RadiologyTypeSearchParams = {}): Observable<PageResponse<RadiologyType>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.active !== undefined) p = p.set('active', String(params.active));
    if (params.modality) p = p.set('modality', params.modality);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<RadiologyType>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<RadiologyType> { return this.http.get<RadiologyType>(`${this.base}/${uid}`); }
  create(req: CreateRadiologyTypeRequest): Observable<RadiologyType> { return this.http.post<RadiologyType>(this.base, req); }
  update(uid: string, req: UpdateRadiologyTypeRequest): Observable<RadiologyType> { return this.http.put<RadiologyType>(`${this.base}/${uid}`, req); }
  setActive(uid: string, active: boolean): Observable<RadiologyType> { return this.http.put<RadiologyType>(`${this.base}/${uid}/active`, { active }); }
  delete(uid: string): Observable<void> { return this.http.delete<void>(`${this.base}/${uid}`); }
}
