import {
  ChangeDetectionStrategy,
  Component,
  type OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { gqlClient } from '@core/graphql/client';
import {
  CreateProviderDocument,
  ProvidersDocument,
  type CreateProviderInput,
  type CreateProviderMutation,
  type CreateProviderMutationVariables,
  type ProviderStatus,
  type ProviderType,
  type ProvidersQuery,
  type ProvidersQueryVariables,
} from '@core/graphql/graphql';
import { toAppError, type AppError } from '@core/graphql/error-handler';
import { AuthService } from '@core/auth/auth.service';
import { providerStatusClass } from '@shared/utils/status';
import { formatDate } from '@shared/utils/date';

type ProviderRow = NonNullable<ProvidersQuery['providers']['items']>[number];

interface StatusOption {
  value: ProviderStatus | '';
  label: string;
}

interface TypeOption {
  value: ProviderType | '';
  label: string;
}

@Component({
  selector: 'app-providers-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './providers-page.component.html',
  styleUrl: './providers-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProvidersPageComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly pageSize = 20;

  private readonly _loading = signal(true);
  private readonly _loadingMore = signal(false);
  private readonly _error = signal<AppError | null>(null);
  private readonly _items = signal<readonly ProviderRow[]>([]);
  private readonly _hasMore = signal(false);
  private readonly _nextCursor = signal<string | null>(null);
  private readonly _status = signal<ProviderStatus | ''>('');
  private readonly _type = signal<ProviderType | ''>('');

  private readonly _createOpen = signal(false);
  private readonly _creating = signal(false);
  private readonly _createError = signal<string | null>(null);
  private readonly _newCode = signal('');
  private readonly _newName = signal('');
  private readonly _newAdapterKey = signal<string>('mock-card');
  private readonly _newType = signal<ProviderType>('CARD');
  private readonly _newConfig = signal('{"behavior":"SUCCESS"}');

  readonly loading = this._loading.asReadonly();
  readonly loadingMore = this._loadingMore.asReadonly();
  readonly error = this._error.asReadonly();
  readonly items = this._items.asReadonly();
  readonly hasMore = this._hasMore.asReadonly();
  readonly status = this._status.asReadonly();
  readonly type = this._type.asReadonly();

  readonly createOpen = this._createOpen.asReadonly();
  readonly creating = this._creating.asReadonly();
  readonly createError = this._createError.asReadonly();
  readonly newCode = this._newCode.asReadonly();
  readonly newName = this._newName.asReadonly();
  readonly newAdapterKey = this._newAdapterKey.asReadonly();
  readonly newType = this._newType.asReadonly();
  readonly newConfig = this._newConfig.asReadonly();

  readonly canManage = computed(() => this.auth.hasPermission('provider:manage:all'));
  readonly isEmpty = computed(
    () => !this._loading() && !this._error() && this._items().length === 0,
  );

  readonly statusOptions: readonly StatusOption[] = [
    { value: '', label: 'All statuses' },
    { value: 'ACTIVE', label: 'Active' },
    { value: 'DEGRADED', label: 'Degraded' },
    { value: 'DISABLED', label: 'Disabled' },
  ];

  readonly typeOptions: readonly TypeOption[] = [
    { value: '', label: 'All types' },
    { value: 'CARD', label: 'Card' },
    { value: 'BANK_TRANSFER', label: 'Bank transfer' },
    { value: 'WALLET', label: 'Wallet' },
  ];

  readonly adapterOptions: readonly string[] = ['mock-card', 'mock-bank', 'mock-wallet'];

  protected readonly formatDate = formatDate;

  ngOnInit(): void {
    void this.reload();
  }

  async reload(): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    this._items.set([]);
    this._hasMore.set(false);
    this._nextCursor.set(null);

    try {
      const result = await gqlClient
        .query<ProvidersQuery, ProvidersQueryVariables>(
          ProvidersDocument,
          this.buildVariables(null),
          { requestPolicy: 'network-only' },
        )
        .toPromise();

      if (result.error) {
        this._error.set(toAppError(result.error));
      } else if (result.data?.providers) {
        this._items.set(result.data.providers.items);
        this._hasMore.set(result.data.providers.hasMore);
        this._nextCursor.set(result.data.providers.nextCursor ?? null);
      }
    } catch (e) {
      this._error.set(toAppError(e));
    } finally {
      this._loading.set(false);
    }
  }

  async loadMore(): Promise<void> {
    const cursor = this._nextCursor();
    if (!cursor || this._loadingMore() || this._loading()) return;

    this._loadingMore.set(true);
    try {
      const result = await gqlClient
        .query<ProvidersQuery, ProvidersQueryVariables>(
          ProvidersDocument,
          this.buildVariables(cursor),
          { requestPolicy: 'network-only' },
        )
        .toPromise();

      if (result.error) {
        this._error.set(toAppError(result.error));
      } else if (result.data?.providers) {
        this._items.update((current) => [...current, ...result.data!.providers.items]);
        this._hasMore.set(result.data.providers.hasMore);
        this._nextCursor.set(result.data.providers.nextCursor ?? null);
      }
    } catch (e) {
      this._error.set(toAppError(e));
    } finally {
      this._loadingMore.set(false);
    }
  }

  onStatusChange(value: string): void {
    this._status.set(value as ProviderStatus | '');
    void this.reload();
  }

  onTypeChange(value: string): void {
    this._type.set(value as ProviderType | '');
    void this.reload();
  }

  statusClass(status: ProviderRow['status']): string {
    return providerStatusClass(status);
  }

  toggleCreate(): void {
    this._createOpen.update((v) => !v);
    this._createError.set(null);
  }

  onNewCodeInput(value: string): void {
    this._newCode.set(value);
  }

  onNewNameInput(value: string): void {
    this._newName.set(value);
  }

  onNewAdapterKeyChange(value: string): void {
    this._newAdapterKey.set(value);
  }

  onNewTypeChange(value: string): void {
    this._newType.set(value as ProviderType);
  }

  onNewConfigInput(value: string): void {
    this._newConfig.set(value);
  }

  async create(): Promise<void> {
    if (this._creating()) return;
    this._creating.set(true);
    this._createError.set(null);

    const input: CreateProviderInput = {
      code: this._newCode().trim(),
      name: this._newName().trim(),
      adapterKey: this._newAdapterKey(),
      type: this._newType(),
      config: this._newConfig().trim() || null,
    };

    try {
      const result = await gqlClient
        .mutation<CreateProviderMutation, CreateProviderMutationVariables>(CreateProviderDocument, { input })
        .toPromise();

      if (result.error) {
        this._createError.set(toAppError(result.error).message);
      } else {
        this._createOpen.set(false);
        this._newCode.set('');
        this._newName.set('');
        this._newAdapterKey.set('mock-card');
        this._newType.set('CARD');
        this._newConfig.set('{"behavior":"SUCCESS"}');
        await this.reload();
      }
    } catch (e) {
      this._createError.set(toAppError(e).message);
    } finally {
      this._creating.set(false);
    }
  }

  private buildVariables(after: string | null): ProvidersQueryVariables {
    const status = this._status();
    const type = this._type();
    return {
      status: status || null,
      type: type || null,
      limit: this.pageSize,
      after,
    };
  }
}
