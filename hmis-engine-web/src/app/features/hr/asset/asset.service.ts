import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import {
  Asset, AssetSearchParams, CreateAssetRequest, RetireAssetRequest, UpdateAssetRequest
} from './asset.types';

@Injectable({ providedIn: 'root' })
export class AssetService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/hr/assets`;

  search(params: AssetSearchParams = {}): Observable<PageResponse<Asset>> {
    let p = new HttpParams();
    if (params.query)    p = p.set('query', params.query);
    if (params.status)   p = p.set('status', params.status);
    if (params.category) p = p.set('category', params.category);
    if (params.location) p = p.set('location', params.location);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort)               p = p.set('sort', params.sort);
    return this.http.get<PageResponse<Asset>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<Asset> {
    return this.http.get<Asset>(`${this.base}/uid/${uid}`);
  }

  findByTag(tag: string): Observable<Asset> {
    return this.http.get<Asset>(`${this.base}/by-tag/${encodeURIComponent(tag)}`);
  }

  create(req: CreateAssetRequest): Observable<Asset> {
    return this.http.post<Asset>(this.base, req);
  }

  update(uid: string, req: UpdateAssetRequest): Observable<Asset> {
    return this.http.put<Asset>(`${this.base}/uid/${uid}`, req);
  }

  retire(uid: string, req: RetireAssetRequest): Observable<Asset> {
    return this.http.post<Asset>(`${this.base}/uid/${uid}/retire`, req);
  }

  reinstate(uid: string): Observable<Asset> {
    return this.http.post<Asset>(`${this.base}/uid/${uid}/reinstate`, {});
  }
}
