import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '@core/auth/auth.service';
import { GraphqlService } from '@core/graphql/client';
import {
  AddProviderCapabilityDocument,
  AddProviderCapabilityInput,
  AddProviderCapabilityMutation,
  AddProviderCapabilityMutationVariables,
  ActivateProviderDocument,
  ActivateProviderMutation,
  ActivateProviderMutationVariables,
  DeleteProviderDocument,
  DeleteProviderMutation,
  DeleteProviderMutationVariables,
  DegradeProviderDocument,
  DegradeProviderMutation,
  DisableProviderDocument,
  DisableProviderMutation,
  MarkProviderCheckedDocument,
  MarkProviderCheckedMutation,
  MarkProviderCheckedMutationVariables,
  PaymentMethod,
  ProviderCapability,
  ProviderCredentialKey,
  ProviderCredentialKeysDocument,
  ProviderCredentialKeysQuery,
  ProviderCredentialKeysQueryVariables,
  ProviderDocument,
  ProviderQuery,
  ProviderQueryVariables,
  ProviderType,
  RemoveProviderCapabilityDocument,
  RemoveProviderCapabilityMutation,
  RemoveProviderCapabilityMutationVariables,
  RotateProviderCredentialDocument,
  RotateProviderCredentialMutation,
  RotateProviderCredentialMutationVariables,
  SetProviderCredentialDocument,
  SetProviderCredentialMutation,
  SetProviderCredentialMutationVariables,
  UpdateProviderCapabilityDocument,
  UpdateProviderCapabilityInput,
  UpdateProviderCapabilityMutation,
  UpdateProviderCapabilityMutationVariables,
  UpdateProviderDocument,
  UpdateProviderInput,
  UpdateProviderMutation,
  UpdateProviderMutationVariables,
} from '@core/graphql/graphql';
import { toErrorMessage } from '@core/graphql/error-handler';
import { formatDate } from '@shared/utils/date';
import { providerStatusClass } from '@shared/utils/status';

@Component({
  selector: 'app-provider-detail',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './provider-detail.component.html',
  styleUrl: './provider-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProviderDetailComponent implements OnInit {
  private readonly graphql = inject(GraphqlService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);

  protected readonly methods: PaymentMethod[] = ['CARD', 'BANK_TRANSFER', 'WALLET'];
  protected readonly types: ProviderType[] = ['CARD', 'BANK_TRANSFER', 'WALLET'];
  protected readonly adapterKeys = ['mock-card', 'mock-bank', 'mock-wallet'] as const;

  private providerId = '';

  protected readonly _loading = signal(false);
  protected readonly _error = signal<string | null>(null);
  protected readonly _provider = signal<ProviderQuery['provider']>(null);
  protected readonly _credentials = signal<ProviderCredentialKey[]>([]);

  protected readonly _actionError = signal<string | null>(null);
  protected readonly _working = signal(false);

  protected readonly _showEdit = signal(false);
  protected readonly _editName = signal('');
  protected readonly _editAdapterKey = signal<string>('mock-card');
  protected readonly _editType = signal<ProviderType>('CARD');
  protected readonly _editConfig = signal('');

  protected readonly _showCapabilityForm = signal(false);
  protected readonly _editingCapabilityId = signal<string | null>(null);
  protected readonly _capMethod = signal<PaymentMethod>('CARD');
  protected readonly _capCurrency = signal('EUR');
  protected readonly _capMin = signal('1.00');
  protected readonly _capMax = signal('10000.00');
  protected readonly _capEnabled = signal(true);

  protected readonly _newCredentialKey = signal('');
  protected readonly _newCredentialValue = signal('');
  protected readonly _rotatingCredentialKey = signal<string | null>(null);
  protected readonly _rotateValue = signal('');

  protected readonly canManage = computed(() => this.auth.hasPermission('provider:manage:all'));

  protected readonly formatDate = formatDate;
  protected readonly providerStatusClass = providerStatusClass;

  ngOnInit(): void {
    this.providerId = this.route.snapshot.paramMap.get('id') ?? '';
    this.reload();
  }

  protected reload(): void {
    if (!this.providerId) {
      this._error.set('Missing provider id');
      return;
    }
    this._loading.set(true);
    this._error.set(null);

    const variables: ProviderQueryVariables = { id: this.providerId };

    this.graphql.query<ProviderQuery, ProviderQueryVariables>(ProviderDocument, variables).subscribe({
      next: (result) => {
        this._provider.set(result.provider);
        if (result.provider) {
          this._editName.set(result.provider.name);
          this._editAdapterKey.set(result.provider.adapterKey);
          this._editType.set(result.provider.type);
          this._editConfig.set(result.provider.config ?? '');
        }
        this._loading.set(false);
        if (this.canManage()) {
          this.loadCredentials();
        }
      },
      error: (err: unknown) => {
        this._error.set(toErrorMessage(err, 'Failed to load provider'));
        this._loading.set(false);
      },
    });
  }

  private loadCredentials(): void {
    const variables: ProviderCredentialKeysQueryVariables = { providerId: this.providerId };
    this.graphql
      .query<ProviderCredentialKeysQuery, ProviderCredentialKeysQueryVariables>(
        ProviderCredentialKeysDocument,
        variables,
      )
      .subscribe({
        next: (result) => this._credentials.set(result.providerCredentialKeys),
        error: (err: unknown) => this._actionError.set(toErrorMessage(err, 'Failed to load credentials')),
      });
  }

  protected toggleEdit(): void {
    this._showEdit.update((v) => !v);
    this._actionError.set(null);
  }

  protected saveEdit(): void {
    const current = this._provider();
    if (!current) {
      return;
    }
    this._working.set(true);
    this._actionError.set(null);

    const input: UpdateProviderInput = {
      name: this._editName().trim(),
      adapterKey: this._editAdapterKey(),
      type: this._editType(),
      config: this._editConfig().trim() || null,
    };

    this.graphql
      .mutate<UpdateProviderMutation, UpdateProviderMutationVariables>(UpdateProviderDocument, {
        id: current.id,
        input,
      })
      .subscribe({
        next: () => {
          this._working.set(false);
          this._showEdit.set(false);
          this.reload();
        },
        error: (err: unknown) => {
          this._actionError.set(toErrorMessage(err, 'Failed to update provider'));
          this._working.set(false);
        },
      });
  }

  protected activate(): void {
    this.runStatus(ActivateProviderDocument, 'activate');
  }

  protected degrade(): void {
    this.runStatus(DegradeProviderDocument, 'degrade');
  }

  protected disable(): void {
    this.runStatus(DisableProviderDocument, 'disable');
  }

  private runStatus(
    doc: typeof ActivateProviderDocument | typeof DegradeProviderDocument | typeof DisableProviderDocument,
    action: string,
  ): void {
    const current = this._provider();
    if (!current) {
      return;
    }
    this._working.set(true);
    this._actionError.set(null);
    this.graphql
      .mutate<ActivateProviderMutation | DegradeProviderMutation | DisableProviderMutation, ActivateProviderMutationVariables>(
        doc,
        { id: current.id },
      )
      .subscribe({
        next: () => {
          this._working.set(false);
          this.reload();
        },
        error: (err: unknown) => {
          this._actionError.set(toErrorMessage(err, `Failed to ${action} provider`));
          this._working.set(false);
        },
      });
  }

  protected markChecked(): void {
    const current = this._provider();
    if (!current) {
      return;
    }
    this._working.set(true);
    this._actionError.set(null);
    this.graphql
      .mutate<MarkProviderCheckedMutation, MarkProviderCheckedMutationVariables>(
        MarkProviderCheckedDocument,
        { id: current.id },
      )
      .subscribe({
        next: () => {
          this._working.set(false);
          this.reload();
        },
        error: (err: unknown) => {
          this._actionError.set(toErrorMessage(err, 'Failed to mark provider checked'));
          this._working.set(false);
        },
      });
  }

  protected deleteProvider(): void {
    const current = this._provider();
    if (!current) {
      return;
    }
    if (!confirm(`Delete provider ${current.code}? This cannot be undone.`)) {
      return;
    }
    this._working.set(true);
    this._actionError.set(null);
    this.graphql
      .mutate<DeleteProviderMutation, DeleteProviderMutationVariables>(DeleteProviderDocument, { id: current.id })
      .subscribe({
        next: () => {
          this._working.set(false);
          globalThis.history.back();
        },
        error: (err: unknown) => {
          this._actionError.set(toErrorMessage(err, 'Failed to delete provider'));
          this._working.set(false);
        },
      });
  }

  protected toggleCapabilityForm(capability?: ProviderCapability): void {
    this._actionError.set(null);
    if (capability) {
      this._editingCapabilityId.set(capability.id);
      this._capMethod.set(capability.method);
      this._capCurrency.set(capability.currency);
      this._capMin.set(capability.minAmount);
      this._capMax.set(capability.maxAmount);
      this._capEnabled.set(capability.enabled);
      this._showCapabilityForm.set(true);
    } else {
      this._editingCapabilityId.set(null);
      this._capMethod.set('CARD');
      this._capCurrency.set('EUR');
      this._capMin.set('1.00');
      this._capMax.set('10000.00');
      this._capEnabled.set(true);
      this._showCapabilityForm.update((v) => !v);
    }
  }

  protected saveCapability(): void {
    const current = this._provider();
    if (!current) {
      return;
    }
    this._working.set(true);
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
      this.graphql
        .mutate<UpdateProviderCapabilityMutation, UpdateProviderCapabilityMutationVariables>(
          UpdateProviderCapabilityDocument,
          { capabilityId: editingId, input },
        )
        .subscribe({
          next: () => {
            this._working.set(false);
            this._showCapabilityForm.set(false);
            this._editingCapabilityId.set(null);
            this.reload();
          },
          error: (err: unknown) => {
            this._actionError.set(toErrorMessage(err, 'Failed to update capability'));
            this._working.set(false);
          },
        });
    } else {
      const input: AddProviderCapabilityInput = {
        method: this._capMethod(),
        currency: this._capCurrency().trim().toUpperCase(),
        minAmount: this._capMin().trim(),
        maxAmount: this._capMax().trim(),
        enabled: this._capEnabled(),
      };
      this.graphql
        .mutate<AddProviderCapabilityMutation, AddProviderCapabilityMutationVariables>(
          AddProviderCapabilityDocument,
          { providerId: current.id, input },
        )
        .subscribe({
          next: () => {
            this._working.set(false);
            this._showCapabilityForm.set(false);
            this.reload();
          },
          error: (err: unknown) => {
            this._actionError.set(toErrorMessage(err, 'Failed to add capability'));
            this._working.set(false);
          },
        });
    }
  }

  protected removeCapability(capability: ProviderCapability): void {
    if (!confirm(`Remove capability ${capability.method}/${capability.currency}?`)) {
      return;
    }
    this._working.set(true);
    this._actionError.set(null);
    this.graphql
      .mutate<RemoveProviderCapabilityMutation, RemoveProviderCapabilityMutationVariables>(
        RemoveProviderCapabilityDocument,
        { capabilityId: capability.id },
      )
      .subscribe({
        next: () => {
          this._working.set(false);
          this.reload();
        },
        error: (err: unknown) => {
          this._actionError.set(toErrorMessage(err, 'Failed to remove capability'));
          this._working.set(false);
        },
      });
  }

  protected setCredential(): void {
    const current = this._provider();
    if (!current) {
      return;
    }
    this._working.set(true);
    this._actionError.set(null);
    this.graphql
      .mutate<SetProviderCredentialMutation, SetProviderCredentialMutationVariables>(
        SetProviderCredentialDocument,
        {
          providerId: current.id,
          input: {
            credentialKey: this._newCredentialKey().trim(),
            value: this._newCredentialValue(),
          },
        },
      )
      .subscribe({
        next: () => {
          this._working.set(false);
          this._newCredentialKey.set('');
          this._newCredentialValue.set('');
          this.reload();
        },
        error: (err: unknown) => {
          this._actionError.set(toErrorMessage(err, 'Failed to set credential'));
          this._working.set(false);
        },
      });
  }

  protected startRotate(credentialKey: string): void {
    this._rotatingCredentialKey.set(credentialKey);
    this._rotateValue.set('');
    this._actionError.set(null);
  }

  protected cancelRotate(): void {
    this._rotatingCredentialKey.set(null);
    this._rotateValue.set('');
  }

  protected rotateCredential(): void {
    const current = this._provider();
    const key = this._rotatingCredentialKey();
    if (!current || !key) {
      return;
    }
    this._working.set(true);
    this._actionError.set(null);
    this.graphql
      .mutate<RotateProviderCredentialMutation, RotateProviderCredentialMutationVariables>(
        RotateProviderCredentialDocument,
        { providerId: current.id, input: { credentialKey: key, value: this._rotateValue() } },
      )
      .subscribe({
        next: () => {
          this._working.set(false);
          this._rotatingCredentialKey.set(null);
          this._rotateValue.set('');
          this.reload();
        },
        error: (err: unknown) => {
          this._actionError.set(toErrorMessage(err, 'Failed to rotate credential'));
          this._working.set(false);
        },
      });
  }
}
