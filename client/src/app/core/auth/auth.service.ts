import { Injectable, signal, computed } from '@angular/core';
import { startAuthentication, startRegistration } from '@simplewebauthn/browser';
import { gqlClient } from '../graphql/client';
import { MeDocument } from '../graphql/graphql';
import { AppError, toAppError } from '../graphql/error-handler';

export interface AppUser {
  id: string;
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

  async loginWithPasskey(email: string): Promise<void> {
    const optionsResponse = await fetch('/webauthn/authenticate/options', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ username: email }),
    });

    if (!optionsResponse.ok) {
      const body = await optionsResponse.json().catch(() => ({}));
      const message = typeof body?.error === 'string' ? body.error : 'Passkey login failed';
      throw new AppError('UNAUTHENTICATED', message);
    }

    const optionsJSON = await optionsResponse.json();

    let credential;
    try {
      credential = await startAuthentication({ optionsJSON });
    } catch (e) {
      const message = e instanceof Error ? e.message : 'Passkey authentication cancelled';
      throw new AppError('UNAUTHENTICATED', message);
    }

    const payload = {
      ...credential,
      clientExtensionResults: undefined,
      response: {
        ...credential.response,
        transports: undefined,
      },
    };

    const verifyResponse = await fetch('/login/webauthn', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(payload),
    });

    if (!verifyResponse.ok) {
      const body = await verifyResponse.json().catch(() => ({}));
      const message = typeof body?.error === 'string' ? body.error : 'Passkey verification failed';
      throw new AppError('UNAUTHENTICATED', message);
    }

    await this.loadSession();
  }

  async enrollPasskey(): Promise<void> {
    const optionsResponse = await fetch('/webauthn/register/options', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
    });

    if (!optionsResponse.ok) {
      const body = await optionsResponse.json().catch(() => ({}));
      const message = typeof body?.error === 'string' ? body.error : 'Failed to get registration options';
      throw new AppError('UNKNOWN', message);
    }

    const optionsJSON = await optionsResponse.json();

    let credential;
    try {
      credential = await startRegistration({ optionsJSON });
    } catch (e) {
      const message = e instanceof Error ? e.message : 'Passkey registration cancelled';
      throw new AppError('UNKNOWN', message);
    }

    const verifyResponse = await fetch('/webauthn/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({
        publicKey: {
          credential: credential,
          label: 'My Passkey',
        },
      }),
    });

    if (!verifyResponse.ok) {
      const body = await verifyResponse.json().catch(() => ({}));
      const message = typeof body?.error === 'string' ? body.error : 'Passkey registration failed';
      throw new AppError('UNKNOWN', message);
    }

    await fetch('/auth/passkey-enrolled', { method: 'POST', credentials: 'include' });
    await this.loadSession();
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
      throw new AppError(
        response.status === 401 ? 'UNAUTHENTICATED' : 'UNKNOWN',
        message,
      );
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
