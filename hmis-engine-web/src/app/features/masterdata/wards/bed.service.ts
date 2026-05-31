import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { Bed, CreateBedRequest, UpdateBedRequest } from './bed.types';

@Injectable({ providedIn: 'root' })
export class BedService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/masterdata`;

  listForWard(wardUid: string): Observable<Bed[]> {
    return this.http.get<Bed[]>(`${this.base}/wards/uid/${wardUid}/beds`);
  }

  create(wardUid: string, req: CreateBedRequest): Observable<Bed> {
    return this.http.post<Bed>(`${this.base}/wards/uid/${wardUid}/beds`, req);
  }

  update(bedUid: string, req: UpdateBedRequest): Observable<Bed> {
    return this.http.put<Bed>(`${this.base}/beds/uid/${bedUid}`, req);
  }

  setActive(bedUid: string, active: boolean): Observable<Bed> {
    return this.http.put<Bed>(`${this.base}/beds/uid/${bedUid}/active`, { active });
  }

  markOutOfService(bedUid: string, reason: string | null): Observable<Bed> {
    return this.http.post<Bed>(`${this.base}/beds/uid/${bedUid}/out-of-service`, { reason });
  }

  markFree(bedUid: string): Observable<Bed> {
    return this.http.post<Bed>(`${this.base}/beds/uid/${bedUid}/free`, {});
  }

  delete(bedUid: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/beds/uid/${bedUid}`);
  }
}
