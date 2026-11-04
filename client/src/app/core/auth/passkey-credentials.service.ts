import { Injectable } from '@angular/core';
import { AppError } from '../graphql/error-handler';

export interface PasskeyCredential {
  credentialId: string;
  label: string;
  created: string | null;
  lastUsed: string | null;
}

@Injectable({ providedIn: 'root' })
export class PasskeyCredentialsService {

  async list(): Promise<PasskeyCredential[]> {
    const response = await fetch('/webauthn/credentials', {
      method: 'GET',
      credentials: 'include',
    });
    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      const message = typeof body?.error === 'string' ? body.error : 'Failed to load passkeys';
      throw new AppError('UNKNOWN', message);
    }
    return response.json();
  }

  async remove(credentialId: string): Promise<void> {
    const response = await fetch(`/webauthn/credentials/${encodeURIComponent(credentialId)}`, {
      method: 'DELETE',
      credentials: 'include',
    });
    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      const message = typeof body?.error === 'string' ? body.error : 'Failed to remove passkey';
      throw new AppError('UNKNOWN', message);
    }
  }
}
