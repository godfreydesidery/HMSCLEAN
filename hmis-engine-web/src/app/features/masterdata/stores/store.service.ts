import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import { CreateStoreRequest, Store, StoreSearchParams, UpdateStoreRequest } from './store.types';

@Injectable({ providedIn: 'root' })
export class StoreService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/masterdata/stores`;

  search(params: StoreSearchParams = {}): Observable<PageResponse<Store>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.active !== undefined) p = p.set('active', String(params.active));
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<Store>>(this.base, { params: p });
  }

  /**
   * The active stores the current keeper is affiliated with (legacy
   * load_stores_by_store_person). Backs the "Select store" workspace picker so a
   * keeper only sees stores they may operate; an unaffiliated user gets an empty list.
   */
  mine(): Observable<Store[]> {
    return this.http.get<Store[]>(`${this.base}/mine`);
  }

  findByUid(uid: string): Observable<Store> { return this.http.get<Store>(`${this.base}/uid/${uid}`); }
  create(req: CreateStoreRequest): Observable<Store> { return this.http.post<Store>(this.base, req); }
  update(uid: string, req: UpdateStoreRequest): Observable<Store> { return this.http.put<Store>(`${this.base}/uid/${uid}`, req); }
  setActive(uid: string, active: boolean): Observable<Store> { return this.http.put<Store>(`${this.base}/uid/${uid}/active`, { active }); }
  delete(uid: string): Observable<void> { return this.http.delete<void>(`${this.base}/uid/${uid}`); }
}
