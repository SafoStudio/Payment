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
  CreateFeeRuleDocument,
  DeleteFeeRuleDocument,
  FeeRulesDocument,
  UpdateFeeRuleDocument,
  type CreateFeeRuleInput,
  type FeeRulesQuery,
  type FeeType,
  type TransactionType,
  type UpdateFeeRuleInput,
} from '@core/graphql/graphql';
import { toAppError, type AppError } from '@core/graphql/error-handler';
import { AuthService } from '@core/auth/auth.service';
import { formatDate } from '@shared/utils/date';

type FeeRuleRow = NonNullable<FeeRulesQuery['feeRules']>[number];

interface FeeTypeOption {
  value: FeeType;
  label: string;
}

interface TransactionTypeOption {
  value: TransactionType;
  label: string;
}

interface RuleForm {
  id: string | null;
  name: string;
  transactionType: TransactionType | '';
  feeType: FeeType | '';
  fixedAmount: string;
  percentage: string;
  minAmount: string;
  maxAmount: string;
  currency: string;
  fromWalletId: string;
  toWalletId: string;
  isActive: boolean;
}

const MONEY_PATTERN = /^\d+(\.\d{1,2})?$/;
const CURRENCY_PATTERN = /^[A-Z]{3}$/;

@Component({
  selector: 'app-fees-page',
  standalone: true,
  templateUrl: './fees-page.component.html',
  styleUrl: './fees-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeesPageComponent implements OnInit {
  private readonly auth = inject(AuthService);

  private readonly _rules = signal<readonly FeeRuleRow[]>([]);
  private readonly _loading = signal(true);
  private readonly _error = signal<AppError | null>(null);

  private readonly _formOpen = signal(false);
  private readonly _form = signal<RuleForm>(emptyForm());
  private readonly _formError = signal<string | null>(null);
  private readonly _formBusy = signal(false);

  private readonly _deleteBusyId = signal<string | null>(null);

  readonly rules = this._rules.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  readonly formOpen = this._formOpen.asReadonly();
  readonly form = this._form.asReadonly();
  readonly formError = this._formError.asReadonly();
  readonly formBusy = this._formBusy.asReadonly();
  readonly deleteBusyId = this._deleteBusyId.asReadonly();

  readonly canManage = computed(() => this.auth.hasPermission('fee_rule:manage'));

  readonly isEmpty = computed(
    () => !this._loading() && !this._error() && this._rules().length === 0,
  );

  readonly feeTypeOptions: readonly FeeTypeOption[] = [
    { value: 'FIXED', label: 'Fixed' },
    { value: 'PERCENTAGE', label: 'Percentage' },
    { value: 'MIXED', label: 'Mixed' },
    { value: 'TIERED', label: 'Tiered (not evaluated)' },
  ];

  readonly transactionTypeOptions: readonly TransactionTypeOption[] = [
    { value: 'TRANSFER', label: 'Transfer' },
    { value: 'TOP_UP', label: 'Top up' },
    { value: 'WITHDRAWAL', label: 'Withdrawal' },
    { value: 'REFUND', label: 'Refund' },
    { value: 'ADJUSTMENT', label: 'Adjustment' },
  ];

  protected readonly formatDate = formatDate;

  ngOnInit(): void {
    void this.load();
  }

  async load(): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const result = await gqlClient
        .query(FeeRulesDocument, {}, { requestPolicy: 'network-only' })
        .toPromise();
      if (result.error) {
        this._error.set(toAppError(result.error));
        this._rules.set([]);
      } else {
        this._rules.set(result.data?.feeRules ?? []);
      }
    } catch (e) {
      this._error.set(toAppError(e));
      this._rules.set([]);
    } finally {
      this._loading.set(false);
    }
  }

  openCreate(): void {
    this._form.set(emptyForm());
    this._formError.set(null);
    this._formOpen.set(true);
  }

  openEdit(rule: FeeRuleRow): void {
    this._form.set({
      id: rule.id,
      name: rule.name,
      transactionType: rule.transactionType as TransactionType,
      feeType: rule.feeType,
      fixedAmount: rule.fixedAmount ?? '',
      percentage: rule.percentage ?? '',
      minAmount: rule.minAmount ?? '',
      maxAmount: rule.maxAmount ?? '',
      currency: rule.currency ?? '',
      fromWalletId: rule.fromWalletId ?? '',
      toWalletId: rule.toWalletId ?? '',
      isActive: rule.isActive,
    });
    this._formError.set(null);
    this._formOpen.set(true);
  }

  closeForm(): void {
    if (this._formBusy()) return;
    this._formOpen.set(false);
    this._formError.set(null);
  }

  onTextInput(key: keyof RuleForm, value: string): void {
    this._form.update((f) => ({ ...f, [key]: value }));
  }

  onFeeTypeChange(value: string): void {
    this._form.update((f) => ({ ...f, feeType: value as FeeType | '' }));
  }

  onTransactionTypeChange(value: string): void {
    this._form.update((f) => ({
      ...f,
      transactionType: value as TransactionType | '',
    }));
  }

  onActiveChange(checked: boolean): void {
    this._form.update((f) => ({ ...f, isActive: checked }));
  }

  async submitForm(): Promise<void> {
    if (this._formBusy()) return;
    const f = this._form();

    if (!f.name.trim()) {
      this._formError.set('Name is required.');
      return;
    }
    if (!f.transactionType) {
      this._formError.set('Transaction type is required.');
      return;
    }
    if (!f.feeType) {
      this._formError.set('Fee type is required.');
      return;
    }
    if (f.fixedAmount && !MONEY_PATTERN.test(f.fixedAmount)) {
      this._formError.set('Fixed amount must be a non-negative number with up to 2 decimals.');
      return;
    }
    if (f.percentage && !MONEY_PATTERN.test(f.percentage)) {
      this._formError.set('Percentage must be a non-negative number with up to 2 decimals.');
      return;
    }
    if (f.minAmount && !MONEY_PATTERN.test(f.minAmount)) {
      this._formError.set('Min amount must be a non-negative number with up to 2 decimals.');
      return;
    }
    if (f.maxAmount && !MONEY_PATTERN.test(f.maxAmount)) {
      this._formError.set('Max amount must be a non-negative number with up to 2 decimals.');
      return;
    }
    if (f.currency && !CURRENCY_PATTERN.test(f.currency)) {
      this._formError.set('Currency must be a 3-letter code (e.g. USD).');
      return;
    }

    this._formBusy.set(true);
    this._formError.set(null);
    try {
      if (f.id) {
        const input: UpdateFeeRuleInput = {
          id: f.id,
          name: f.name.trim(),
          transactionType: f.transactionType,
          feeType: f.feeType,
          fixedAmount: nullIfBlank(f.fixedAmount),
          percentage: nullIfBlank(f.percentage),
          minAmount: nullIfBlank(f.minAmount),
          maxAmount: nullIfBlank(f.maxAmount),
          currency: nullIfBlank(f.currency),
          fromWalletId: nullIfBlank(f.fromWalletId),
          toWalletId: nullIfBlank(f.toWalletId),
          isActive: f.isActive,
        };
        const result = await gqlClient
          .mutation(UpdateFeeRuleDocument, { input })
          .toPromise();
        if (result.error) {
          this._formError.set(toAppError(result.error).message);
          return;
        }
      } else {
        const input: CreateFeeRuleInput = {
          name: f.name.trim(),
          transactionType: f.transactionType,
          feeType: f.feeType,
          fixedAmount: nullIfBlank(f.fixedAmount),
          percentage: nullIfBlank(f.percentage),
          minAmount: nullIfBlank(f.minAmount),
          maxAmount: nullIfBlank(f.maxAmount),
          currency: nullIfBlank(f.currency),
          fromWalletId: nullIfBlank(f.fromWalletId),
          toWalletId: nullIfBlank(f.toWalletId),
          isActive: f.isActive,
        };
        const result = await gqlClient
          .mutation(CreateFeeRuleDocument, { input })
          .toPromise();
        if (result.error) {
          this._formError.set(toAppError(result.error).message);
          return;
        }
      }
      this._formOpen.set(false);
      await this.load();
    } catch (e) {
      this._formError.set(toAppError(e).message);
    } finally {
      this._formBusy.set(false);
    }
  }

  async deleteRule(rule: FeeRuleRow): Promise<void> {
    if (this._deleteBusyId()) return;
    const confirmed = globalThis.confirm(
      `Delete fee rule "${rule.name}"? This cannot be undone.`,
    );
    if (!confirmed) return;

    this._deleteBusyId.set(rule.id);
    this._error.set(null);
    try {
      const result = await gqlClient
        .mutation(DeleteFeeRuleDocument, { id: rule.id })
        .toPromise();
      if (result.error) {
        this._error.set(toAppError(result.error));
        return;
      }
      await this.load();
    } catch (e) {
      this._error.set(toAppError(e));
    } finally {
      this._deleteBusyId.set(null);
    }
  }
}

function emptyForm(): RuleForm {
  return {
    id: null,
    name: '',
    transactionType: '',
    feeType: '',
    fixedAmount: '',
    percentage: '',
    minAmount: '',
    maxAmount: '',
    currency: '',
    fromWalletId: '',
    toWalletId: '',
    isActive: true,
  };
}

function nullIfBlank(value: string): string | null {
  const v = value.trim();
  return v === '' ? null : v;
}
