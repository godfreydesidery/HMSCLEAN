import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';

export type LoginOutcome = 'SUCCESS' | 'BAD_CREDENTIALS' | 'USER_DISABLED' | 'USER_LOCKED' | 'UNKNOWN_USER';

export const LOGIN_OUTCOMES: { value: LoginOutcome; label: string; badgeClass: string }[] = [
  { value: 'SUCCESS',         label: 'Success',         badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'BAD_CREDENTIALS', label: 'Bad credentials', badgeClass: 'text-bg-danger-subtle text-danger-emphasis border border-danger-subtle' },
  { value: 'USER_DISABLED',   label: 'User disabled',   badgeClass: 'text-bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle' },
  { value: 'USER_LOCKED',     label: 'User locked',     badgeClass: 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle' },
  { value: 'UNKNOWN_USER',    label: 'Unknown user',    badgeClass: 'text-bg-dark-subtle text-dark-emphasis border border-dark-subtle' }
];

export interface LoginAttempt {
  uid: string;
  username: string;
  outcome: LoginOutcome;
  ipAddress: string | null;
  userAgent: string | null;
  attemptedAt: string;
}

export interface LoginAttemptSearchParams {
  username?: string;
  outcome?: LoginOutcome;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/iam/audit`;

  searchLoginAttempts(params: LoginAttemptSearchParams = {}): Observable<PageResponse<LoginAttempt>> {
    let p = new HttpParams();
    if (params.username) p = p.set('username', params.username);
    if (params.outcome) p = p.set('outcome', params.outcome);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<LoginAttempt>>(`${this.base}/login-attempts`, { params: p });
  }
}
