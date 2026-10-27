import {
  ChangeDetectionStrategy,
  Component,
  type OnInit,
  computed,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { gqlClient } from '@core/graphql/client';
import {
  TransactionsDocument,
  type TransactionStatus,
  type TransactionType,
  type TransactionsQuery,
  type TransactionsQueryVariables,
} from '@core/graphql/graphql';
import { toAppError, type AppError } from '@core/graphql/error-handler';

type TransactionRow = NonNullable<TransactionsQuery['transactions']['items']>[number];

interface TypeOption {
  value: TransactionType | '';
  label: string;
}

interface StatusOption {
  value: TransactionStatus | '';
  label: string;
}

@Component({
  selector: 'app-transactions-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './transactions-page.component.html',
  styleUrl: './transactions-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TransactionsPageComponent implements OnInit {
  private readonly pageSize = 20;

  private readonly _loading = signal(true);
  private readonly _loadingMore = signal(false);
  private readonly _error = signal<AppError | null>(null);
  private readonly _items = signal<readonly TransactionRow[]>([]);
  private readonly _hasMore = signal(false);
  private readonly _nextCursor = signal<string | null>(null);
  private readonly _type = signal<TransactionType | ''>('');
  private readonly _status = signal<TransactionStatus | ''>('');

  readonly loading = this._loading.asReadonly();
  readonly loadingMore = this._loadingMore.asReadonly();
  readonly error = this._error.asReadonly();
  readonly items = this._items.asReadonly();
  readonly hasMore = this._hasMore.asReadonly();
  readonly type = this._type.asReadonly();
  readonly status = this._status.asReadonly();

  readonly typeOptions: readonly TypeOption[] = [
    { value: '', label: 'All types' },
    { value: 'TRANSFER', label: 'Transfer' },
    { value: 'TOP_UP', label: 'Top up' },
    { value: 'WITHDRAWAL', label: 'Withdrawal' },
    { value: 'REFUND', label: 'Refund' },
  ];

  readonly statusOptions: readonly StatusOption[] = [
    { value: '', label: 'All statuses' },
    { value: 'PENDING', label: 'Pending' },
    { value: 'COMPLETED', label: 'Completed' },
    { value: 'FAILED', label: 'Failed' },
    { value: 'CANCELLED', label: 'Cancelled' },
  ];

  readonly isEmpty = computed(
    () =>
      !this._loading() &&
      !this._error() &&
      this._items().length === 0,
  );

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
        .query(TransactionsDocument, this.buildVariables(null), {
          requestPolicy: 'network-only',
        })
        .toPromise();

      if (result.error) {
        this._error.set(toAppError(result.error));
      } else if (result.data?.transactions) {
        this._items.set(result.data.transactions.items);
        this._hasMore.set(result.data.transactions.hasMore);
        this._nextCursor.set(result.data.transactions.nextCursor ?? null);
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
        .query(TransactionsDocument, this.buildVariables(cursor), {
          requestPolicy: 'network-only',
        })
        .toPromise();

      if (result.error) {
        this._error.set(toAppError(result.error));
      } else if (result.data?.transactions) {
        this._items.update((current) => [
          ...current,
          ...result.data!.transactions.items,
        ]);
        this._hasMore.set(result.data.transactions.hasMore);
        this._nextCursor.set(result.data.transactions.nextCursor ?? null);
      }
    } catch (e) {
      this._error.set(toAppError(e));
    } finally {
      this._loadingMore.set(false);
    }
  }

  onTypeChange(value: string): void {
    this._type.set(value as TransactionType | '');
    void this.reload();
  }

  onStatusChange(value: string): void {
    this._status.set(value as TransactionStatus | '');
    void this.reload();
  }

  formatAmount(amount: string, currency: string): string {
    const value = Number(amount);
    if (!Number.isFinite(value)) return amount;
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency,
    }).format(value);
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleString(undefined, {
      year: 'numeric',
      month: 'short',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  statusClass(status: TransactionRow['status']): string {
    switch (status) {
      case 'COMPLETED':
        return 'transactions__status--completed';
      case 'FAILED':
      case 'CANCELLED':
        return 'transactions__status--failed';
      case 'PENDING':
        return 'transactions__status--pending';
      default:
        return '';
    }
  }

  private buildVariables(after: string | null): TransactionsQueryVariables {
    const type = this._type();
    const status = this._status();
    const filter =
      type || status
        ? {
            ...(type ? { type } : {}),
            ...(status ? { status } : {}),
          }
        : null;

    return {
      filter,
      limit: this.pageSize,
      after,
    };
  }
}
