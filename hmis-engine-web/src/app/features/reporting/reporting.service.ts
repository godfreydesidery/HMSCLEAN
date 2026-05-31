import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  BedOccupancyEntry,
  ExpiringBatchEntry,
  IpdRegisterEntry,
  IpdRegisterParams,
  RevenueReportParams,
  RevenueSummaryDto,
  StockOutEntry
} from './reporting.types';

@Injectable({ providedIn: 'root' })
export class ReportingService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/reporting`;

  revenue(params: RevenueReportParams): Observable<RevenueSummaryDto> {
    const p = new HttpParams().set('from', params.from).set('to', params.to);
    return this.http.get<RevenueSummaryDto>(`${this.base}/revenue`, { params: p });
  }

  ipdRegister(params: IpdRegisterParams): Observable<IpdRegisterEntry[]> {
    let p = new HttpParams().set('from', params.from).set('to', params.to);
    if (params.wardUid) p = p.set('wardUid', params.wardUid);
    if (params.status) p = p.set('status', params.status);
    return this.http.get<IpdRegisterEntry[]>(`${this.base}/ipd-register`, { params: p });
  }

  bedOccupancy(): Observable<BedOccupancyEntry[]> {
    return this.http.get<BedOccupancyEntry[]>(`${this.base}/bed-occupancy`);
  }

  stockOut(threshold = 0): Observable<StockOutEntry[]> {
    const p = new HttpParams().set('threshold', String(threshold));
    return this.http.get<StockOutEntry[]>(`${this.base}/stock-out`, { params: p });
  }

  expiringBatches(daysAhead = 30): Observable<ExpiringBatchEntry[]> {
    const p = new HttpParams().set('daysAhead', String(daysAhead));
    return this.http.get<ExpiringBatchEntry[]>(`${this.base}/expiring-batches`, { params: p });
  }
}
