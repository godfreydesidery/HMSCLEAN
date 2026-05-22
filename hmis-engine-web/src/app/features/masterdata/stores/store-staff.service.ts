import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { StoreStaff } from './store-staff.types';

/**
 * The store keeper ⇄ store affiliation: which keepers work at a store. Issuing
 * goods out of a store is restricted to its affiliated keepers.
 */
@Injectable({ providedIn: 'root' })
export class StoreStaffService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/masterdata/stores`;

  list(storeUid: string): Observable<StoreStaff[]> {
    return this.http.get<StoreStaff[]>(`${this.base}/uid/${storeUid}/staff`);
  }

  assign(storeUid: string, userUid: string): Observable<StoreStaff> {
    return this.http.post<StoreStaff>(`${this.base}/uid/${storeUid}/staff`, { userUid });
  }

  remove(storeUid: string, userUid: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/uid/${storeUid}/staff/uid/${userUid}`);
  }
}
