import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AdjustConsumableRequest, Consumable, ConsumableIssue, ConsumableSearchParams,
  ConsumableSourceKind, ConsumableStockBalanceDto, CreateConsumableRequest, IssueConsumableRequest,
  PageResponse, ReceiveConsumableRequest, UpdateConsumableRequest
} from './consumable.types';

/** Consumable masterdata. */
@Injectable({ providedIn: 'root' })
export class ConsumableMasterdataService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/masterdata/consumables`;

  search(params: ConsumableSearchParams = {}): Observable<PageResponse<Consumable>> {
    let p = new HttpParams();
    if (params.query)            p = p.set('query', params.query);
    if (params.active !== undefined) p = p.set('active', String(params.active));
    if (params.page !== undefined)   p = p.set('page', String(params.page));
    if (params.size !== undefined)   p = p.set('size', String(params.size));
    if (params.sort)             p = p.set('sort', params.sort);
    return this.http.get<PageResponse<Consumable>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<Consumable> { return this.http.get<Consumable>(`${this.base}/uid/${uid}`); }
  create(req: CreateConsumableRequest): Observable<Consumable> { return this.http.post<Consumable>(this.base, req); }
  update(uid: string, req: UpdateConsumableRequest): Observable<Consumable> { return this.http.put<Consumable>(`${this.base}/uid/${uid}`, req); }
  setActive(uid: string, active: boolean): Observable<Consumable> { return this.http.put<Consumable>(`${this.base}/uid/${uid}/active`, { active }); }
  delete(uid: string): Observable<void> { return this.http.delete<void>(`${this.base}/uid/${uid}`); }
}

/** Patient consumable chart (issues against an admission). */
@Injectable({ providedIn: 'root' })
export class ConsumableIssueService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/encounters/admissions`;

  listForAdmission(admissionUid: string): Observable<ConsumableIssue[]> {
    return this.http.get<ConsumableIssue[]>(`${this.base}/uid/${admissionUid}/consumables`);
  }

  issue(admissionUid: string, req: IssueConsumableRequest): Observable<ConsumableIssue> {
    return this.http.post<ConsumableIssue>(`${this.base}/uid/${admissionUid}/consumables`, req);
  }
}

/** Source-location stock for consumables. */
@Injectable({ providedIn: 'root' })
export class ConsumableStockService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/consumables/stock`;

  listBySource(sourceKind: ConsumableSourceKind, sourceUid: string): Observable<ConsumableStockBalanceDto[]> {
    const p = new HttpParams().set('sourceKind', sourceKind).set('sourceUid', sourceUid);
    return this.http.get<ConsumableStockBalanceDto[]>(`${this.base}/by-source`, { params: p });
  }

  receive(req: ReceiveConsumableRequest): Observable<ConsumableStockBalanceDto> {
    return this.http.post<ConsumableStockBalanceDto>(`${this.base}/receive`, req);
  }

  adjust(req: AdjustConsumableRequest): Observable<ConsumableStockBalanceDto> {
    return this.http.post<ConsumableStockBalanceDto>(`${this.base}/adjust`, req);
  }
}
