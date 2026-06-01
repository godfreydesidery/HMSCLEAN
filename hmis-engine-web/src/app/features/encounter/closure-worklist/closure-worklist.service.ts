import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import { ClosureSubject, ClosureWorklistItem } from './closure-worklist.types';

@Injectable({ providedIn: 'root' })
export class ClosureWorklistService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/encounters/closures`;

  /** PENDING closures awaiting a second approver, optionally scoped to one subject type. */
  worklist(params: { subjectType?: ClosureSubject | ''; page?: number; size?: number } = {})
      : Observable<PageResponse<ClosureWorklistItem>> {
    let p = new HttpParams();
    if (params.subjectType) p = p.set('subjectType', params.subjectType);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    return this.http.get<PageResponse<ClosureWorklistItem>>(`${this.base}/worklist`, { params: p });
  }
}
