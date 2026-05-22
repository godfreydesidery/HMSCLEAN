import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { ProviderProfile, UpsertProviderProfileRequest } from './provider-profile.types';

/**
 * The clinician's specialty / registration / licence sidecar. Surfaces in the
 * staff pickers so a consultation can be booked against "Dr. X — Cardiology".
 */
@Injectable({ providedIn: 'root' })
export class ProviderProfileService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/iam/users`;

  get(userUid: string): Observable<ProviderProfile> {
    return this.http.get<ProviderProfile>(`${this.base}/uid/${userUid}/provider-profile`);
  }

  upsert(userUid: string, req: UpsertProviderProfileRequest): Observable<ProviderProfile> {
    return this.http.put<ProviderProfile>(`${this.base}/uid/${userUid}/provider-profile`, req);
  }
}
