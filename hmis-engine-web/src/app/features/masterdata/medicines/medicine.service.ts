import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import {
  CreateMedicineRequest, Medicine, MedicineSearchParams, UpdateMedicineRequest
} from './medicine.types';

@Injectable({ providedIn: 'root' })
export class MedicineService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/masterdata/medicines`;

  search(params: MedicineSearchParams = {}): Observable<PageResponse<Medicine>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.active !== undefined) p = p.set('active', String(params.active));
    if (params.form) p = p.set('form', params.form);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<Medicine>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<Medicine> { return this.http.get<Medicine>(`${this.base}/uid/${uid}`); }
  create(req: CreateMedicineRequest): Observable<Medicine> { return this.http.post<Medicine>(this.base, req); }
  update(uid: string, req: UpdateMedicineRequest): Observable<Medicine> { return this.http.put<Medicine>(`${this.base}/uid/${uid}`, req); }
  setActive(uid: string, active: boolean): Observable<Medicine> { return this.http.put<Medicine>(`${this.base}/uid/${uid}/active`, { active }); }
  delete(uid: string): Observable<void> { return this.http.delete<void>(`${this.base}/uid/${uid}`); }
}
