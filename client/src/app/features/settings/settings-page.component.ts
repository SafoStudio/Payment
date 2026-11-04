import {
  ChangeDetectionStrategy,
  Component,
  type OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { gqlClient } from '@core/graphql/client';
import {
  MeProfileDocument,
  UpdateMeDocument,
  type MeProfileQuery,
  type UpdateMeInput,
} from '@core/graphql/graphql';
import { toAppError, AppError } from '@core/graphql/error-handler';
import { AuthService } from '@core/auth/auth.service';
import { PasskeyCredentialsService, type PasskeyCredential } from '@core/auth/passkey-credentials.service';
import { formatDate } from '@shared/utils/date';

type Profile = NonNullable<MeProfileQuery['me']>;

@Component({
  selector: 'app-settings-page',
  standalone: true,
  templateUrl: './settings-page.component.html',
  styleUrl: './settings-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SettingsPageComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly passkeysApi = inject(PasskeyCredentialsService);

  private readonly _profile = signal<Profile | null>(null);
  private readonly _loading = signal(true);
  private readonly _error = signal<AppError | null>(null);

  private readonly _phone = signal('');
  private readonly _firstName = signal('');
  private readonly _lastName = signal('');
  private readonly _dirty = signal(false);

  private readonly _saving = signal(false);
  private readonly _saveError = signal<string | null>(null);
  private readonly _savedAt = signal<string | null>(null);

  private readonly _passkeys = signal<readonly PasskeyCredential[]>([]);
  private readonly _passkeysLoading = signal(true);
  private readonly _passkeysError = signal<string | null>(null);
  private readonly _passkeyBusyId = signal<string | null>(null);
  private readonly _passkeyAddBusy = signal(false);

  readonly profile = this._profile.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  readonly phone = this._phone.asReadonly();
  readonly firstName = this._firstName.asReadonly();
  readonly lastName = this._lastName.asReadonly();
  readonly dirty = this._dirty.asReadonly();

  readonly saving = this._saving.asReadonly();
  readonly saveError = this._saveError.asReadonly();
  readonly savedAt = this._savedAt.asReadonly();

  readonly passkeys = this._passkeys.asReadonly();
  readonly passkeysLoading = this._passkeysLoading.asReadonly();
  readonly passkeysError = this._passkeysError.asReadonly();
  readonly passkeyBusyId = this._passkeyBusyId.asReadonly();
  readonly passkeyAddBusy = this._passkeyAddBusy.asReadonly();

  readonly canSave = computed(() => this._dirty() && !this._saving());

  protected readonly formatDate = formatDate;

  ngOnInit(): void {
    void this.load();
    void this.loadPasskeys();
  }

  async load(): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const result = await gqlClient
        .query(MeProfileDocument, {}, { requestPolicy: 'network-only' })
        .toPromise();
      if (result.error) {
        this._error.set(toAppError(result.error));
      } else if (result.data?.me) {
        this.applyProfile(result.data.me);
      }
    } catch (e) {
      this._error.set(toAppError(e));
    } finally {
      this._loading.set(false);
    }
  }

  async loadPasskeys(): Promise<void> {
    this._passkeysLoading.set(true);
    this._passkeysError.set(null);
    try {
      this._passkeys.set(await this.passkeysApi.list());
    } catch (e) {
      this._passkeysError.set(e instanceof AppError ? e.message : 'Failed to load passkeys');
    } finally {
      this._passkeysLoading.set(false);
    }
  }

  onPhoneInput(value: string): void {
    this._phone.set(value);
    this._dirty.set(true);
  }

  onFirstNameInput(value: string): void {
    this._firstName.set(value);
    this._dirty.set(true);
  }

  onLastNameInput(value: string): void {
    this._lastName.set(value);
    this._dirty.set(true);
  }

  reset(): void {
    const p = this._profile();
    if (!p) return;
    this.applyProfile(p);
    this._saveError.set(null);
    this._savedAt.set(null);
  }

  async save(): Promise<void> {
    if (this._saving() || !this._dirty()) return;

    const phone = this._phone().trim();
    const firstName = this._firstName().trim();
    const lastName = this._lastName().trim();

    if (!firstName) {
      this._saveError.set('First name is required.');
      return;
    }
    if (!lastName) {
      this._saveError.set('Last name is required.');
      return;
    }

    const input: UpdateMeInput = {
      phone: phone === '' ? null : phone,
      firstName,
      lastName,
    };

    this._saving.set(true);
    this._saveError.set(null);
    try {
      const result = await gqlClient
        .mutation(UpdateMeDocument, { input })
        .toPromise();
      if (result.error) {
        this._saveError.set(toAppError(result.error).message);
        return;
      }
      this._savedAt.set(new Date().toISOString());
      await this.load();
    } catch (e) {
      this._saveError.set(toAppError(e).message);
    } finally {
      this._saving.set(false);
    }
  }

  async addPasskey(): Promise<void> {
    if (this._passkeyAddBusy()) return;
    this._passkeyAddBusy.set(true);
    this._passkeysError.set(null);
    try {
      await this.auth.enrollPasskey();
      await this.loadPasskeys();
      await this.load();
    } catch (e) {
      this._passkeysError.set(e instanceof AppError ? e.message : 'Failed to add passkey');
    } finally {
      this._passkeyAddBusy.set(false);
    }
  }

  async revokePasskey(credentialId: string): Promise<void> {
    if (this._passkeyBusyId()) return;
    const confirmed = globalThis.confirm(
      'Revoke this passkey? It will stop working for signing in on this device. ' +
      'You can remove the local copy later from your browser or OS.',
    );
    if (!confirmed) return;

    this._passkeyBusyId.set(credentialId);
    this._passkeysError.set(null);
    try {
      await this.passkeysApi.remove(credentialId);
      await this.loadPasskeys();
    } catch (e) {
      this._passkeysError.set(e instanceof AppError ? e.message : 'Failed to revoke passkey');
    } finally {
      this._passkeyBusyId.set(null);
    }
  }

  private applyProfile(p: Profile): void {
    this._profile.set(p);
    this._phone.set(p.phone ?? '');
    this._firstName.set(p.firstName);
    this._lastName.set(p.lastName);
    this._dirty.set(false);
  }
}
