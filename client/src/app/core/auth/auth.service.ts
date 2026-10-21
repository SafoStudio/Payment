import { Injectable, signal, computed } from '@angular/core';
import { gqlClient } from '../graphql/client';
import { MeDocument } from '../graphql/graphql';
import { toAppError, type AppError } from '../graphql/error-handler';

export interface AppUser {
  id: string;
  email: string;
  roles: string[];
  permissions: string[];
  hasPasskey: boolean;
}

interface LoginResponse {
  userId: string;
  email: string;
  roles: string[];
  permissions: string[];
  hasPasskey: boolean;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly _user = signal<AppUser | null>(null);
  private readonly _loading = signal(true);
  private readonly _lastError = signal<AppError | null>(null);

  readonly user = this._user.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly lastError = this._lastError.asReadonly();

  readonly isAuthenticated = computed(() => this._user() !== null);

  hasPermission(code: string): boolean {
    return this._user()?.permissions.includes(code) ?? false;
  }

  hasAnyPermission(codes: string[]): boolean {
    const user = this._user();
    if (!user) return false;
    return codes.some((c) => user.permissions.includes(c));
  }

  hasRole(code: string): boolean {
    return this._user()?.roles.includes(code) ?? false;
  }

  async loadSession(): Promise<void> {
    this._loading.set(true);
    this._lastError.set(null);

    const result = await gqlClient
      .query(MeDocument, {}, { requestPolicy: 'network-only' })
      .toPromise();

    if (result.error) {
      const appError = toAppError(result.error);
      if (appError.type !== 'UNAUTHENTICATED') {
        this._lastError.set(appError);
      }
      this._user.set(null);
    } else if (result.data?.me) {
      this._user.set({
        id: result.data.me.id,
        email: result.data.me.email,
        roles: result.data.me.roles,
        permissions: result.data.me.permissions,
        hasPasskey: result.data.me.hasPasskey ?? false,
      });
    } else {
      this._user.set(null);
    }

    this._loading.set(false);
  }

  async loginWithPassword(email: string, password: string): Promise<void> {
    const response = await fetch('/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ email, password }),
    });

    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      const message = typeof body?.error === 'string' ? body.error : 'Login failed';
      throw {
        type: response.status === 401 ? 'UNAUTHENTICATED' : 'UNKNOWN',
        message,
      } satisfies AppError;
    }

    await this.loadSession();
  }

  async logout(): Promise<void> {
    try {
      await fetch('/auth/logout', {
        method: 'POST',
        credentials: 'include',
      });
    } finally {
      this.clearSession();
    }
  }

  clearSession(): void {
    this._user.set(null);
    this._lastError.set(null);
  }
}
