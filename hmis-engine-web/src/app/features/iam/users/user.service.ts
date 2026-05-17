import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import { CreateUserRequest, Role, User, UserSearchParams } from './user.types';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/iam`;

  search(params: UserSearchParams = {}): Observable<PageResponse<User>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.enabled !== undefined) p = p.set('enabled', String(params.enabled));
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<User>>(`${this.base}/users`, { params: p });
  }

  findByUid(uid: string): Observable<User> { return this.http.get<User>(`${this.base}/users/${uid}`); }
  create(req: CreateUserRequest): Observable<User> { return this.http.post<User>(`${this.base}/users`, req); }
  setEnabled(uid: string, enabled: boolean): Observable<User> {
    return this.http.put<User>(`${this.base}/users/${uid}/enabled`, { enabled });
  }
  replaceRoles(uid: string, roleNames: string[]): Observable<User> {
    return this.http.put<User>(`${this.base}/users/${uid}/roles`, roleNames);
  }
  resetPassword(uid: string, newPassword: string): Observable<User> {
    return this.http.post<User>(`${this.base}/users/${uid}/reset-password`, { newPassword });
  }
  unlock(uid: string): Observable<User> {
    return this.http.post<User>(`${this.base}/users/${uid}/unlock`, {});
  }

  listRoles(): Observable<Role[]> { return this.http.get<Role[]>(`${this.base}/roles`); }
}
