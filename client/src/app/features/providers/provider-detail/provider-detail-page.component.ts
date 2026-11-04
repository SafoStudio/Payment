import {
  ChangeDetectionStrategy,
  Component,
  type OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { gqlClient } from '@core/graphql/client';
import {
  AddProviderCapabilityDocument,
  ActivateProviderDocument,
  DeleteProviderDocument,
  DegradeProviderDocument,
  DisableProviderDocument,
  MarkProviderCheckedDocument,
  ProviderCredentialKeysDocument,
  ProviderDocument,
  RemoveProviderCapabilityDocument,
  RotateProviderCredentialDocument,
  SetProviderCredentialDocument,
  UpdateProviderCapabilityDocument,
  UpdateProviderDocument,
  type AddProviderCapabilityInput,
  type AddProviderCapabilityMutation,
  type AddProviderCapabilityMutationVariables,
  type ActivateProviderMutation,
  type ActivateProviderMutationVariables,
  type DegradeProviderMutation,
  type DegradeProviderMutationVariables,
  type DeleteProviderMutation,
  type DeleteProviderMutationVariables,
  type DisableProviderMutation,
  type DisableProviderMutationVariables,
  type MarkProviderCheckedMutation,
  type MarkProviderCheckedMutationVariables,
  type PaymentMethod,
  type ProviderCapability,
  type ProviderCredentialKey,
  type ProviderCredentialKeysQuery,
  type ProviderCredentialKeysQueryVariables,
  type ProviderQuery,
  type ProviderQueryVariables,
  type ProviderType,
  type RemoveProviderCapabilityMutation,
  type RemoveProviderCapabilityMutationVariables,
  type RotateProviderCredentialMutation,
  type RotateProviderCredentialMutationVariables,
  type SetProviderCredentialMutation,
  type SetProviderCredentialMutationVariables,
  type UpdateProviderCapabilityInput,
  type UpdateProviderCapabilityMutation,
  type UpdateProviderCapabilityMutationVariables,
  type UpdateProviderInput,
  type UpdateProviderMutation,
  type UpdateProviderMutationVariables,
} from '@core/graphql/graphql';
import { toAppError, type AppError } from '@core/graphql/error-handler';
import { AuthService } from '@core/auth/auth.service';
import { providerStatusClass } from '@shared/utils/status';
import { formatDate } from '@shared/utils/date';

type Provider = NonNullable<ProviderQuery['provider']>;
type Capability = NonNullable<ProviderQuery['provider']>['capabilities'][number];

@Component({
  selector: 'app-provider-detail-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './provider-detail-page.component.html',
  styleUrl: './provider-detail-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProviderDetailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly providerId = this.route.snapshot.paramMap.get('id') ?? '';

  private readonly _provider = signal<Provider | null>(null);
  private readonly _credentials = signal<readonly ProviderCredentialKey[]>([]);
  private readonly _loading = signal(true);
  private readonly _error = signal<AppError | null>(null);

  private readonly _actionBusy = signal(false);
  private readonly _actionError = signal<string | null>(null);

  private readonly _editOpen = signal(false);
  private readonly _editName = signal('');
  private readonly _editAdapterKey = signal<string>('mock-card');
  private readonly _editType = signal<ProviderType>('CARD');
  private readonly _editConfig = signal('');

  private readonly _capFormOpen = signal(false);
  private readonly _editingCapabilityId = signal<string | null>(null);
  private readonly _capMethod = signal<PaymentMethod>('CARD');
  private readonly _capCurrency = signal('EUR');
  private readonly _capMin = signal('1.00');
  private readonly _capMax = signal('10000.00');
  private readonly _capEnabled = signal(true);

  private readonly _newCredentialKey = signal('');
  private readonly _newCredentialValue = signal('');
  private readonly _rotatingKey = signal<string | null>(null);
  private readonly _rotateValue = signal('');

  readonly provider = this._provider.asReadonly();
  readonly credentials = this._credentials.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly actionBusy = this._actionBusy.asReadonly();
  readonly actionError = this._actionError.asReadonly();
  readonly editOpen = this._editOpen.asReadonly();
  readonly editName = this._editName.asReadonly();
  readonly editAdapterKey = this._editAdapterKey.asReadonly();
  readonly editType = this._editType.asReadonly();
  readonly editConfig = this._editConfig.asReadonly();
  readonly capFormOpen = this._capFormOpen.asReadonly();
  readonly editingCapabilityId = this._editingCapabilityId.asReadonly();
  readonly capMethod = this._capMethod.asReadonly();
  readonly capCurrency = this._capCurrency.asReadonly();
  readonly capMin = this._capMin.asReadonly();
  readonly capMax = this._capMax.asReadonly();
  readonly capEnabled = this._capEnabled.asReadonly();
  readonly newCredentialKey = this._newCredentialKey.asReadonly();
  readonly newCredentialValue = this._newCredentialValue.asReadonly();
  readonly rotatingKey = this._rotatingKey.asReadonly();
  readonly rotateValue = this._rotateValue.asReadonly();

  readonly canManage = computed(() => this.auth.hasPermission('provider:manage:all'));

  readonly typeOptions: readonly ProviderType[] = ['CARD', 'BANK_TRANSFER', 'WALLET'];
  readonly methodOptions: readonly PaymentMethod[] = ['CARD', 'BANK_TRANSFER', 'WALLET'];
  readonly adapterOptions: readonly string[] = ['mock-card', 'mock-bank', 'mock-wallet'];

  protected readonly formatDate = formatDate;

  ngOnInit(): void {
    void this.load();
  }

  async load(): Promise<void> {
    if (!this.providerId) {
      this._error.set(toAppError(new Error('Missing provider id')));
      this._loading.set(false);
      return;
    }
    this._loading.set(true);
    this._error.set(null);
    try {
      const result = await gqlClient
        .query<ProviderQuery, ProviderQueryVariables>(
          ProviderDocument,
          { id: this.providerId },
          { requestPolicy: 'network-only' },
        )
        .toPromise();

      if (result.error) {
        this._error.set(toAppError(result.error));
        return;
      }
      const provider = result.data?.provider ?? null;
      this._provider.set(provider);
      if (provider) {
        this._editName.set(provider.name);
        this._editAdapterKey.set(provider.adapterKey);
        this._editType.set(provider.type);
        this._editConfig.set(provider.config ?? '');
      }
      if (this.canManage()) {
        await this.loadCredentials();
      }
    } catch (e) {
      this._error.set(toAppError(e));
    } finally {
      this._loading.set(false);
    }
  }

  async reloadProvider(): Promise<void> {
    try {
      const result = await gqlClient
        .query<ProviderQuery, ProviderQueryVariables>(
          ProviderDocument,
          { id: this.providerId },
          { requestPolicy: 'network-only' },
        )
        .toPromise();
      if (result.error) {
        this._actionError.set(toAppError(result.error).message);
      } else if (result.data?.provider) {
        this._provider.set(result.data.provider);
      }
    } catch (e) {
      this._actionError.set(toAppError(e).message);
    }
  }

  private async loadCredentials(): Promise<void> {
    try {
      const result = await gqlClient
        .query<ProviderCredentialKeysQuery, ProviderCredentialKeysQueryVariables>(
          ProviderCredentialKeysDocument,
          { providerId: this.providerId },
          { requestPolicy: 'network-only' },
        )
        .toPromise();
      if (result.error) {
        this._actionError.set(toAppError(result.error).message);
      } else if (result.data?.providerCredentialKeys) {
        this._credentials.set(result.data.providerCredentialKeys);
      }
    } catch (e) {
      this._actionError.set(toAppError(e).message);
    }
  }

  statusClass(status: string): string {
    return providerStatusClass(status);
  }

  toggleEdit(): void {
    this._editOpen.update((v) => !v);
    this._actionError.set(null);
  }

  onEditNameInput(value: string): void {
    this._editName.set(value);
  }

  onEditAdapterChange(value: string): void {
    this._editAdapterKey.set(value);
  }

  onEditTypeChange(value: string): void {
    this._editType.set(value as ProviderType);
  }

  onEditConfigInput(value: string): void {
    this._editConfig.set(value);
  }

  async saveEdit(): Promise<void> {
    const current = this._provider();
    if (!current || this._actionBusy()) return;
    this._actionBusy.set(true);
    this._actionError.set(null);

    const input: UpdateProviderInput = {
      name: this._editName().trim(),
      adapterKey: this._editAdapterKey(),
      type: this._editType(),
      config: this._editConfig().trim() || null,
    };

    try {
      const result = await gqlClient
        .mutation<UpdateProviderMutation, UpdateProviderMutationVariables>(UpdateProviderDocument, {
          id: current.id,
          input,
        })
        .toPromise();
      if (result.error) {
        this._actionError.set(toAppError(result.error).message);
      } else {
        this._editOpen.set(false);
        await this.reloadProvider();
      }
    } catch (e) {
      this._actionError.set(toAppError(e).message);
    } finally {
      this._actionBusy.set(false);
    }
  }

  async activate(): Promise<void> {
    await this.runStatus(ActivateProviderDocument, 'activate');
  }

  async degrade(): Promise<void> {
    await this.runStatus(DegradeProviderDocument, 'degrade');
  }

  async disable(): Promise<void> {
    await this.runStatus(DisableProviderDocument, 'disable');
  }

  private async runStatus(
    doc: typeof ActivateProviderDocument | typeof DegradeProviderDocument | typeof DisableProviderDocument,
    action: string,
  ): Promise<void> {
    const current = this._provider();
    if (!current || this._actionBusy()) return;
    this._actionBusy.set(true);
    this._actionError.set(null);
    try {
      const result = await gqlClient
        .mutation<
          ActivateProviderMutation | DegradeProviderMutation | DisableProviderMutation,
          ActivateProviderMutationVariables
        >(doc, { id: current.id })
        .toPromise();
      if (result.error) {
        this._actionError.set(toAppError(result.error).message);
      } else {
        await this.reloadProvider();
      }
    } catch (e) {
      this._actionError.set(toAppError(e).message);
    } finally {
      this._actionBusy.set(false);
    }
  }

  async markChecked(): Promise<void> {
    const current = this._provider();
    if (!current || this._actionBusy()) return;
    this._actionBusy.set(true);
    this._actionError.set(null);
    try {
      const result = await gqlClient
        .mutation<MarkProviderCheckedMutation, MarkProviderCheckedMutationVariables>(
          MarkProviderCheckedDocument,
          { id: current.id },
        )
        .toPromise();
      if (result.error) {
        this._actionError.set(toAppError(result.error).message);
      } else {
        await this.reloadProvider();
      }
    } catch (e) {
      this._actionError.set(toAppError(e).message);
    } finally {
      this._actionBusy.set(false);
    }
  }

  async deleteProvider(): Promise<void> {
    const current = this._provider();
    if (!current || this._actionBusy()) return;
    if (!confirm(`Delete provider ${current.code}? This cannot be undone.`)) return;

    this._actionBusy.set(true);
    this._actionError.set(null);
    try {
      const result = await gqlClient
        .mutation<DeleteProviderMutation, DeleteProviderMutationVariables>(DeleteProviderDocument, {
          id: current.id,
        })
        .toPromise();
      if (result.error) {
        this._actionError.set(toAppError(result.error).message);
      } else {
        await this.router.navigate(['/providers']);
      }
    } catch (e) {
      this._actionError.set(toAppError(e).message);
    } finally {
      this._actionBusy.set(false);
    }
  }

  openCapabilityForm(capability?: Capability): void {
    this._actionError.set(null);
    if (capability) {
      this._editingCapabilityId.set(capability.id);
      this._capMethod.set(capability.method);
      this._capCurrency.set(capability.currency);
      this._capMin.set(capability.minAmount);
      this._capMax.set(capability.maxAmount);
      this._capEnabled.set(capability.enabled);
      this._capFormOpen.set(true);
    } else {
      this._editingCapabilityId.set(null);
      this._capMethod.set('CARD');
      this._capCurrency.set('EUR');
      this._capMin.set('1.00');
      this._capMax.set('10000.00');
      this._capEnabled.set(true);
      this._capFormOpen.update((v) => !v);
    }
  }

  closeCapabilityForm(): void {
    if (this._actionBusy()) return;
    this._capFormOpen.set(false);
    this._editingCapabilityId.set(null);
    this._actionError.set(null);
  }

  onCapMethodChange(value: string): void {
    this._capMethod.set(value as PaymentMethod);
  }

  onCapCurrencyInput(value: string): void {
    this._capCurrency.set(value);
  }

  onCapMinInput(value: string): void {
    this._capMin.set(value);
  }

  onCapMaxInput(value: string): void {
    this._capMax.set(value);
  }

  onCapEnabledChange(value: boolean): void {
    this._capEnabled.set(value);
  }

  async saveCapability(): Promise<void> {
    const current = this._provider();
    if (!current || this._actionBusy()) return;
    this._actionBusy.set(true);
    this._actionError.set(null);

    const editingId = this._editingCapabilityId();

    if (editingId) {
      const input: UpdateProviderCapabilityInput = {
        method: this._capMethod(),
        currency: this._capCurrency().trim().toUpperCase(),
        minAmount: this._capMin().trim(),
        maxAmount: this._capMax().trim(),
        enabled: this._capEnabled(),
      };
      try {
        const result = await gqlClient
          .mutation<UpdateProviderCapabilityMutation, UpdateProviderCapabilityMutationVariables>(
            UpdateProviderCapabilityDocument,
            { capabilityId: editingId, input },
          )
          .toPromise();
        if (result.error) {
          this._actionError.set(toAppError(result.error).message);
        } else {
          this._capFormOpen.set(false);
          this._editingCapabilityId.set(null);
          await this.reloadProvider();
        }
      } catch (e) {
        this._actionError.set(toAppError(e).message);
      } finally {
        this._actionBusy.set(false);
      }
    } else {
      const input: AddProviderCapabilityInput = {
        method: this._capMethod(),
        currency: this._capCurrency().trim().toUpperCase(),
        minAmount: this._capMin().trim(),
        maxAmount: this._capMax().trim(),
        enabled: this._capEnabled(),
      };
      try {
        const result = await gqlClient
          .mutation<AddProviderCapabilityMutation, AddProviderCapabilityMutationVariables>(
            AddProviderCapabilityDocument,
            { providerId: current.id, input },
          )
          .toPromise();
        if (result.error) {
          this._actionError.set(toAppError(result.error).message);
        } else {
          this._capFormOpen.set(false);
          await this.reloadProvider();
        }
      } catch (e) {
        this._actionError.set(toAppError(e).message);
      } finally {
        this._actionBusy.set(false);
      }
    }
  }

  async removeCapability(capability: Capability): Promise<void> {
    if (!confirm(`Remove capability ${capability.method}/${capability.currency}?`)) return;
    this._actionBusy.set(true);
    this._actionError.set(null);
    try {
      const result = await gqlClient
        .mutation<RemoveProviderCapabilityMutation, RemoveProviderCapabilityMutationVariables>(
          RemoveProviderCapabilityDocument,
          { capabilityId: capability.id },
        )
        .toPromise();
      if (result.error) {
        this._actionError.set(toAppError(result.error).message);
      } else {
        await this.reloadProvider();
      }
    } catch (e) {
      this._actionError.set(toAppError(e).message);
    } finally {
      this._actionBusy.set(false);
    }
  }

  onNewCredentialKeyInput(value: string): void {
    this._newCredentialKey.set(value);
  }

  onNewCredentialValueInput(value: string): void {
    this._newCredentialValue.set(value);
  }

  async setCredential(): Promise<void> {
    const current = this._provider();
    if (!current || this._actionBusy()) return;
    this._actionBusy.set(true);
    this._actionError.set(null);
    try {
      const result = await gqlClient
        .mutation<SetProviderCredentialMutation, SetProviderCredentialMutationVariables>(
          SetProviderCredentialDocument,
          {
            providerId: current.id,
            input: {
              credentialKey: this._newCredentialKey().trim(),
              value: this._newCredentialValue(),
            },
          },
        )
        .toPromise();
      if (result.error) {
        this._actionError.set(toAppError(result.error).message);
      } else {
        this._newCredentialKey.set('');
        this._newCredentialValue.set('');
        await Promise.all([this.reloadProvider(), this.loadCredentials()]);
      }
    } catch (e) {
      this._actionError.set(toAppError(e).message);
    } finally {
      this._actionBusy.set(false);
    }
  }

  startRotate(credentialKey: string): void {
    this._rotatingKey.set(credentialKey);
    this._rotateValue.set('');
    this._actionError.set(null);
  }

  cancelRotate(): void {
    this._rotatingKey.set(null);
    this._rotateValue.set('');
  }

  onRotateValueInput(value: string): void {
    this._rotateValue.set(value);
  }

  async rotateCredential(): Promise<void> {
    const current = this._provider();
    const key = this._rotatingKey();
    if (!current || !key || this._actionBusy()) return;
    this._actionBusy.set(true);
    this._actionError.set(null);
    try {
      const result = await gqlClient
        .mutation<RotateProviderCredentialMutation, RotateProviderCredentialMutationVariables>(
          RotateProviderCredentialDocument,
          { providerId: current.id, input: { credentialKey: key, value: this._rotateValue() } },
        )
        .toPromise();
      if (result.error) {
        this._actionError.set(toAppError(result.error).message);
      } else {
        this._rotatingKey.set(null);
        this._rotateValue.set('');
        await Promise.all([this.reloadProvider(), this.loadCredentials()]);
      }
    } catch (e) {
      this._actionError.set(toAppError(e).message);
    } finally {
      this._actionBusy.set(false);
    }
  }

  back(): void {
    void this.router.navigate(['/providers']);
  }
}
