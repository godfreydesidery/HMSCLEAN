import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';

export interface StaffOption {
  uid: string;
  username: string;
  firstName: string;
  lastName: string;
  fullName: string;
}

@Injectable({ providedIn: 'root' })
export class StaffDirectoryService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/iam/staff`;

  byRole(roleName: string): Observable<StaffOption[]> {
    return this.http.get<StaffOption[]>(`${this.base}/by-role/${encodeURIComponent(roleName)}`);
  }
}
