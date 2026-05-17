import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ChangePasswordRequest, LoginRequest, LoginResponse, TokenPair, UserSummary } from './auth.types';

const STORAGE_KEY = 'hmis.session';

interface PersistedSession {
  tokens: TokenPair;
  user: UserSummary;
  roles: string[];
  privileges: string[];
  passwordMustChange: boolean;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly sessionSignal = signal<PersistedSession | null>(this.readStorage());

  readonly session = this.sessionSignal.asReadonly();
  readonly user = computed(() => this.sessionSignal()?.user ?? null);
  readonly isAuthenticated = computed(() => {
    const s = this.sessionSignal();
    if (!s) {
      return false;
    }
    return !this.isExpired(s.tokens.accessToken);
  });
  readonly privileges = computed(() => this.sessionSignal()?.privileges ?? []);
  readonly roles = computed(() => this.sessionSignal()?.roles ?? []);
  readonly passwordMustChange = computed(() => this.sessionSignal()?.passwordMustChange ?? false);

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${environment.apiUrl}/auth/login`, request)
      .pipe(tap((response) => this.persist(response)));
  }

  changePassword(request: ChangePasswordRequest): Observable<void> {
    return this.http
      .post<void>(`${environment.apiUrl}/auth/change-password`, request)
      .pipe(tap(() => {
        // The server has invalidated all prior refresh tokens. Clear the
        // must-change flag locally; the access token is still valid until
        // it expires, after which the user will re-log in cleanly.
        const s = this.sessionSignal();
        if (s) this.sessionSignal.set({ ...s, passwordMustChange: false });
      }));
  }

  logout(): void {
    const refresh = this.sessionSignal()?.tokens.refreshToken;
    if (refresh) {
      this.http.post(`${environment.apiUrl}/auth/logout`, { refreshToken: refresh })
        .subscribe({ next: () => {}, error: () => {} });
    }
    localStorage.removeItem(STORAGE_KEY);
    this.sessionSignal.set(null);
    void this.router.navigate(['/login']);
  }

  accessToken(): string | null {
    return this.sessionSignal()?.tokens.accessToken ?? null;
  }

  hasPrivilege(...required: string[]): boolean {
    if (required.length === 0) {
      return true;
    }
    const owned = this.privileges();
    return required.some((p) => owned.includes(p));
  }

  hasRole(...required: string[]): boolean {
    if (required.length === 0) {
      return true;
    }
    const owned = this.roles();
    return required.some((r) => owned.includes(r));
  }

  private persist(response: LoginResponse): void {
    const session: PersistedSession = {
      tokens: response.tokens,
      user: response.user,
      roles: response.roles,
      privileges: response.privileges,
      passwordMustChange: response.passwordMustChange
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    this.sessionSignal.set(session);
  }

  private readStorage(): PersistedSession | null {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as PersistedSession;
    } catch {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }

  private isExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1])) as { exp?: number };
      if (!payload.exp) {
        return false;
      }
      return Date.now() >= payload.exp * 1000;
    } catch {
      return true;
    }
  }
}
