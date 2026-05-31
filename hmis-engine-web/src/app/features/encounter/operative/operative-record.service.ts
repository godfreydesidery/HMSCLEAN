import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  CreateAmendmentRequest, OperativeRecord, OperativeRecordAmendment, UpsertOperativeRecordRequest
} from './operative-record.types';

@Injectable({ providedIn: 'root' })
export class OperativeRecordService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/encounters/orders/uid`;

  private url(orderUid: string): string {
    return `${this.base}/${orderUid}/operative-record`;
  }

  /** GET the record for an order. The endpoint may 404 / return null when none exists yet. */
  find(orderUid: string): Observable<OperativeRecord | null> {
    return this.http.get<OperativeRecord | null>(this.url(orderUid))
      .pipe(catchError(() => of(null)));
  }

  /** Create-or-update the editable fields (only valid while UNLOCKED). */
  upsert(orderUid: string, req: UpsertOperativeRecordRequest): Observable<OperativeRecord> {
    return this.http.put<OperativeRecord>(this.url(orderUid), req);
  }

  /** Lock the record — turns it read-only. */
  lock(orderUid: string): Observable<OperativeRecord> {
    return this.http.post<OperativeRecord>(`${this.url(orderUid)}/lock`, {});
  }

  listAmendments(orderUid: string): Observable<OperativeRecordAmendment[]> {
    return this.http.get<OperativeRecordAmendment[]>(`${this.url(orderUid)}/amendments`);
  }

  addAmendment(orderUid: string, req: CreateAmendmentRequest): Observable<OperativeRecordAmendment> {
    return this.http.post<OperativeRecordAmendment>(`${this.url(orderUid)}/amendments`, req);
  }
}
