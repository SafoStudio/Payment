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
  CreateRoutingRuleDocument,
  DeleteRoutingRuleDocument,
  DisableRoutingRuleDocument,
  EnableRoutingRuleDocument,
  ProvidersDocument,
  ResolveProviderDocument,
  RoutingRulesDocument,
  UpdateRoutingRuleDocument,
  type CreateRoutingRuleInput,
  type CreateRoutingRuleMutation,
  type CreateRoutingRuleMutationVariables,
  type DeleteRoutingRuleMutation,
  type DeleteRoutingRuleMutationVariables,
  type DisableRoutingRuleMutation,
  type DisableRoutingRuleMutationVariables,
  type EnableRoutingRuleMutation,
  type EnableRoutingRuleMutationVariables,
  type PaymentMethod,
  type ProvidersQuery,
  type ProvidersQueryVariables,
  type ResolveProviderQuery,
  type ResolveProviderQueryVariables,
  type RoutingRulesQuery,
  type RoutingRulesQueryVariables,
  type UpdateRoutingRuleInput,
  type UpdateRoutingRuleMutation,
  type UpdateRoutingRuleMutationVariables,
} from '@core/graphql/graphql';
import { toAppError, type AppError } from '@core/graphql/error-handler';
import { AuthService } from '@core/auth/auth.service';
import { formatDate } from '@shared/utils/date';

type RoutingRuleRow = NonNullable<RoutingRulesQuery['routingRules']['items']>[number];
type ResolvedProvider = NonNullable<ResolveProviderQuery['resolveProvider']>;
type EnabledFilterValue = '' | 'true' | 'false';

interface MethodOption {
  value: PaymentMethod | '';
  label: string;
}

interface EnabledOption {
  value: EnabledFilterValue;
  label: string;
}

interface ProviderOption {
  value: string;
  label: string;
}

@Component({
  selector: 'app-routing-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './routing-page.component.html',
  styleUrl: './routing-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RoutingPageComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly pageSize = 20;

  private readonly _loading = signal(true);
  private readonly _loadingMore = signal(false);
  private readonly _error = signal<AppError | null>(null);
  private readonly _items = signal<readonly RoutingRuleRow[]>([]);
  private readonly _hasMore = signal(false);
  private readonly _nextCursor = signal<string | null>(null);

  private readonly _method = signal<PaymentMethod | ''>('');
  private readonly _currency = signal('');
  private readonly _enabled = signal<EnabledFilterValue>('');

  private readonly _createOpen = signal(false);
  private readonly _creating = signal(false);
  private readonly _createError = signal<string | null>(null);
  private readonly _newMethod = signal<PaymentMethod>('CARD');
  private readonly _newCurrency = signal('EUR');
  private readonly _newProviderId = signal('');
  private readonly _newPriority = signal('100');
  private readonly _newWeight = signal('100');

  private readonly _providerOptions = signal<readonly ProviderOption[]>([]);
  private readonly _providersLoading = signal(false);
  private readonly _providersError = signal<string | null>(null);

  private readonly _editingId = signal<string | null>(null);
  private readonly _editPriority = signal('');
  private readonly _editWeight = signal('');
  private readonly _editEnabled = signal(true);
  private readonly _saving = signal(false);
  private readonly _editError = signal<string | null>(null);

  private readonly _actionBusyId = signal<string | null>(null);
  private readonly _actionError = signal<string | null>(null);

  private readonly _resolveOpen = signal(false);
  private readonly _resolveMethod = signal<PaymentMethod>('CARD');
  private readonly _resolveCurrency = signal('EUR');
  private readonly _resolveAmount = signal('100.00');
  private readonly _resolving = signal(false);
  private readonly _resolveError = signal<string | null>(null);
  private readonly _resolved = signal<ResolvedProvider | null>(null);

  readonly loading = this._loading.asReadonly();
  readonly loadingMore = this._loadingMore.asReadonly();
  readonly error = this._error.asReadonly();
  readonly items = this._items.asReadonly();
  readonly hasMore = this._hasMore.asReadonly();
  readonly method = this._method.asReadonly();
  readonly currency = this._currency.asReadonly();
  readonly enabled = this._enabled.asReadonly();

  readonly createOpen = this._createOpen.asReadonly();
  readonly creating = this._creating.asReadonly();
  readonly createError = this._createError.asReadonly();
  readonly newMethod = this._newMethod.asReadonly();
  readonly newCurrency = this._newCurrency.asReadonly();
  readonly newProviderId = this._newProviderId.asReadonly();
  readonly newPriority = this._newPriority.asReadonly();
  readonly newWeight = this._newWeight.asReadonly();

  readonly providerOptions = this._providerOptions.asReadonly();
  readonly providersLoading = this._providersLoading.asReadonly();
  readonly providersError = this._providersError.asReadonly();

  readonly editingId = this._editingId.asReadonly();
  readonly editPriority = this._editPriority.asReadonly();
  readonly editWeight = this._editWeight.asReadonly();
  readonly editEnabled = this._editEnabled.asReadonly();
  readonly saving = this._saving.asReadonly();
  readonly editError = this._editError.asReadonly();

  readonly actionBusyId = this._actionBusyId.asReadonly();
  readonly actionError = this._actionError.asReadonly();

  readonly resolveOpen = this._resolveOpen.asReadonly();
  readonly resolveMethod = this._resolveMethod.asReadonly();
  readonly resolveCurrency = this._resolveCurrency.asReadonly();
  readonly resolveAmount = this._resolveAmount.asReadonly();
  readonly resolving = this._resolving.asReadonly();
  readonly resolveError = this._resolveError.asReadonly();
  readonly resolved = this._resolved.asReadonly();

  readonly canManage = computed(() => this.auth.hasPermission('routing:manage:all'));
  readonly isEmpty = computed(
    () => !this._loading() && !this._error() && this._items().length === 0,
  );

  readonly methodOptions: readonly MethodOption[] = [
    { value: '', label: 'All methods' },
    { value: 'CARD', label: 'Card' },
    { value: 'BANK_TRANSFER', label: 'Bank transfer' },
    { value: 'WALLET', label: 'Wallet' },
  ];

  readonly methodOptionsRequired: readonly { value: PaymentMethod; label: string }[] = [
    { value: 'CARD', label: 'Card' },
    { value: 'BANK_TRANSFER', label: 'Bank transfer' },
    { value: 'WALLET', label: 'Wallet' },
  ];

  readonly enabledOptions: readonly EnabledOption[] = [
    { value: '', label: 'All' },
    { value: 'true', label: 'Enabled' },
    { value: 'false', label: 'Disabled' },
  ];

  protected readonly formatDate = formatDate;

  ngOnInit(): void {
    void this.reload();
    void this.loadProviderOptions();
  }

  async reload(): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    this._items.set([]);
    this._hasMore.set(false);
    this._nextCursor.set(null);

    try {
      const result = await gqlClient
        .query<RoutingRulesQuery, RoutingRulesQueryVariables>(
          RoutingRulesDocument,
          this.buildVariables(null),
          { requestPolicy: 'network-only' },
        )
        .toPromise();

      if (result.error) {
        this._error.set(toAppError(result.error));
      } else if (result.data?.routingRules) {
        this._items.set(result.data.routingRules.items);
        this._hasMore.set(result.data.routingRules.hasMore);
        this._nextCursor.set(result.data.routingRules.nextCursor ?? null);
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
        .query<RoutingRulesQuery, RoutingRulesQueryVariables>(
          RoutingRulesDocument,
          this.buildVariables(cursor),
          { requestPolicy: 'network-only' },
        )
        .toPromise();

      if (result.error) {
        this._error.set(toAppError(result.error));
      } else if (result.data?.routingRules) {
        this._items.update((current) => [...current, ...result.data!.routingRules.items]);
        this._hasMore.set(result.data.routingRules.hasMore);
        this._nextCursor.set(result.data.routingRules.nextCursor ?? null);
      }
    } catch (e) {
      this._error.set(toAppError(e));
    } finally {
      this._loadingMore.set(false);
    }
  }

  private async loadProviderOptions(): Promise<void> {
    this._providersLoading.set(true);
    this._providersError.set(null);
    try {
      const result = await gqlClient
        .query<ProvidersQuery, ProvidersQueryVariables>(
          ProvidersDocument,
          { limit: 100 },
          { requestPolicy: 'network-only' },
        )
        .toPromise();

      if (result.error) {
        this._providersError.set(toAppError(result.error).message);
      } else if (result.data?.providers) {
        const options = result.data.providers.items.map((p) => ({
          value: p.id,
          label: `${p.code} — ${p.name}`,
        }));
        this._providerOptions.set(options);
        if (!this._newProviderId() && options.length > 0) {
          this._newProviderId.set(options[0].value);
        }
      }
    } catch (e) {
      this._providersError.set(toAppError(e).message);
    } finally {
      this._providersLoading.set(false);
    }
  }

  onMethodChange(value: string): void {
    this._method.set(value as PaymentMethod | '');
    void this.reload();
  }

  onCurrencyChange(value: string): void {
    const normalized = value.trim().toUpperCase();
    this._currency.set(normalized);
    void this.reload();
  }

  onEnabledChange(value: string): void {
    this._enabled.set(value as EnabledFilterValue);
    void this.reload();
  }

  enabledClass(enabled: boolean): string {
    return enabled ? 'status--active' : 'status--disabled';
  }

  toggleCreate(): void {
    this._createOpen.update((v) => !v);
    this._createError.set(null);
  }

  onNewMethodChange(value: string): void {
    this._newMethod.set(value as PaymentMethod);
  }

  onNewCurrencyInput(value: string): void {
    this._newCurrency.set(value.toUpperCase());
  }

  onNewProviderIdInput(value: string): void {
    this._newProviderId.set(value);
  }

  onNewPriorityInput(value: string): void {
    this._newPriority.set(value);
  }

  onNewWeightInput(value: string): void {
    this._newWeight.set(value);
  }

  async create(): Promise<void> {
    if (this._creating()) return;
    this._creating.set(true);
    this._createError.set(null);

    const input: CreateRoutingRuleInput = {
      method: this._newMethod(),
      currency: this._newCurrency().trim().toUpperCase(),
      providerId: this._newProviderId().trim(),
      priority: Number.parseInt(this._newPriority(), 10) || 0,
      weight: Number.parseInt(this._newWeight(), 10) || 0,
    };

    try {
      const result = await gqlClient
        .mutation<CreateRoutingRuleMutation, CreateRoutingRuleMutationVariables>(
          CreateRoutingRuleDocument,
          { input },
        )
        .toPromise();

      if (result.error) {
        this._createError.set(toAppError(result.error).message);
      } else {
        this._createOpen.set(false);
        this._newMethod.set('CARD');
        this._newCurrency.set('EUR');
        this._newProviderId.set(this._providerOptions()[0]?.value ?? '');
        this._newPriority.set('100');
        this._newWeight.set('100');
        await this.reload();
      }
    } catch (e) {
      this._createError.set(toAppError(e).message);
    } finally {
      this._creating.set(false);
    }
  }

  openEdit(rule: RoutingRuleRow): void {
    this._editingId.set(rule.id);
    this._editPriority.set(String(rule.priority));
    this._editWeight.set(String(rule.weight));
    this._editEnabled.set(rule.enabled);
    this._editError.set(null);
  }

  cancelEdit(): void {
    this._editingId.set(null);
    this._editError.set(null);
  }

  onEditPriorityInput(value: string): void {
    this._editPriority.set(value);
  }

  onEditWeightInput(value: string): void {
    this._editWeight.set(value);
  }

  onEditEnabledChange(value: string): void {
    this._editEnabled.set(value === 'true');
  }

  async saveEdit(): Promise<void> {
    const id = this._editingId();
    if (!id || this._saving()) return;

    this._saving.set(true);
    this._editError.set(null);

    const input: UpdateRoutingRuleInput = {
      priority: Number.parseInt(this._editPriority(), 10) || 0,
      weight: Number.parseInt(this._editWeight(), 10) || 0,
      enabled: this._editEnabled(),
    };

    try {
      const result = await gqlClient
        .mutation<UpdateRoutingRuleMutation, UpdateRoutingRuleMutationVariables>(
          UpdateRoutingRuleDocument,
          { id, input },
        )
        .toPromise();

      if (result.error) {
        this._editError.set(toAppError(result.error).message);
      } else {
        this._editingId.set(null);
        await this.reload();
      }
    } catch (e) {
      this._editError.set(toAppError(e).message);
    } finally {
      this._saving.set(false);
    }
  }

  async toggleEnabled(rule: RoutingRuleRow): Promise<void> {
    if (this._actionBusyId()) return;
    this._actionBusyId.set(rule.id);
    this._actionError.set(null);

    try {
      const doc = rule.enabled ? DisableRoutingRuleDocument : EnableRoutingRuleDocument;
      const result = await gqlClient
        .mutation<
          DisableRoutingRuleMutation | EnableRoutingRuleMutation,
          DisableRoutingRuleMutationVariables | EnableRoutingRuleMutationVariables
        >(doc, { id: rule.id })
        .toPromise();

      if (result.error) {
        this._actionError.set(toAppError(result.error).message);
      } else {
        await this.reload();
      }
    } catch (e) {
      this._actionError.set(toAppError(e).message);
    } finally {
      this._actionBusyId.set(null);
    }
  }

  async delete(rule: RoutingRuleRow): Promise<void> {
    if (this._actionBusyId()) return;
    if (!confirm(`Delete routing rule ${rule.method}/${rule.currency} → ${rule.providerCode ?? rule.providerId}?`)) {
      return;
    }

    this._actionBusyId.set(rule.id);
    this._actionError.set(null);

    try {
      const result = await gqlClient
        .mutation<DeleteRoutingRuleMutation, DeleteRoutingRuleMutationVariables>(
          DeleteRoutingRuleDocument,
          { id: rule.id },
        )
        .toPromise();

      if (result.error) {
        this._actionError.set(toAppError(result.error).message);
      } else {
        await this.reload();
      }
    } catch (e) {
      this._actionError.set(toAppError(e).message);
    } finally {
      this._actionBusyId.set(null);
    }
  }

  toggleResolve(): void {
    this._resolveOpen.update((v) => !v);
    this._resolveError.set(null);
    this._resolved.set(null);
  }

  onResolveMethodChange(value: string): void {
    this._resolveMethod.set(value as PaymentMethod);
  }

  onResolveCurrencyInput(value: string): void {
    this._resolveCurrency.set(value.toUpperCase());
  }

  onResolveAmountInput(value: string): void {
    this._resolveAmount.set(value);
  }

  async resolve(): Promise<void> {
    if (this._resolving()) return;
    this._resolving.set(true);
    this._resolveError.set(null);
    this._resolved.set(null);

    try {
      const result = await gqlClient
        .query<ResolveProviderQuery, ResolveProviderQueryVariables>(
          ResolveProviderDocument,
          {
            method: this._resolveMethod(),
            currency: this._resolveCurrency().trim().toUpperCase(),
            amount: this._resolveAmount().trim(),
          },
          { requestPolicy: 'network-only' },
        )
        .toPromise();

      if (result.error) {
        this._resolveError.set(toAppError(result.error).message);
      } else if (result.data?.resolveProvider) {
        this._resolved.set(result.data.resolveProvider);
      }
    } catch (e) {
      this._resolveError.set(toAppError(e).message);
    } finally {
      this._resolving.set(false);
    }
  }

  private buildVariables(after: string | null): RoutingRulesQueryVariables {
    const method = this._method();
    const currency = this._currency();
    const enabled = this._enabled();
    return {
      method: method || null,
      currency: currency || null,
      enabled: enabled === '' ? null : enabled === 'true',
      limit: this.pageSize,
      after,
    };
  }
}
