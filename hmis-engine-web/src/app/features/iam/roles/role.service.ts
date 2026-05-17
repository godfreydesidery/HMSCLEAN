import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { CreateRoleRequest, Privilege, Role } from './role.types';

@Injectable({ providedIn: 'root' })
export class RoleService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/iam`;

  list(): Observable<Role[]> { return this.http.get<Role[]>(`${this.base}/roles`); }
  findByUid(uid: string): Observable<Role> { return this.http.get<Role>(`${this.base}/roles/uid/${uid}`); }
  create(req: CreateRoleRequest): Observable<Role> { return this.http.post<Role>(`${this.base}/roles`, req); }
  replacePrivileges(uid: string, privileges: string[]): Observable<Role> {
    return this.http.put<Role>(`${this.base}/roles/uid/${uid}/privileges`, privileges);
  }

  listPrivileges(): Observable<Privilege[]> { return this.http.get<Privilege[]>(`${this.base}/privileges`); }
}
