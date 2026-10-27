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
  PaymentsDocument,
  type PaymentStatus,
  type PaymentsQuery,
  type PaymentsQueryVariables,
} from '@core/graphql/graphql';
import { toAppError, type AppError } from '@core/graphql/error-handler';

type PaymentRow = NonNullable<PaymentsQuery['payments']['items']>[number];

interface StatusOption {
  value: PaymentStatus | '';
  label: string;
}

@Component({
  selector: 'app-payments-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './payments-page.component.html',
  styleUrl: './payments-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentsPageComponent implements OnInit {
  private readonly pageSize = 20;

  private readonly _loading = signal(true);
  private readonly _loadingMore = signal(false);
  private readonly _error = signal<AppError | null>(null);
  private readonly _items = signal<readonly PaymentRow[]>([]);
  private readonly _hasMore = signal(false);
  private readonly _nextCursor = signal<string | null>(null);
  private readonly _status = signal<PaymentStatus | ''>('');

  readonly loading = this._loading.asReadonly();
  readonly loadingMore = this._loadingMore.asReadonly();
  readonly error = this._error.asReadonly();
  readonly items = this._items.asReadonly();
  readonly hasMore = this._hasMore.asReadonly();
  readonly status = this._status.asReadonly();

  readonly statusOptions: readonly StatusOption[] = [
    { value: '', label: 'All statuses' },
    { value: 'PENDING', label: 'Pending' },
    { value: 'PROCESSING', label: 'Processing' },
    { value: 'SUCCEEDED', label: 'Succeeded' },
    { value: 'FAILED', label: 'Failed' },
    { value: 'REFUNDED', label: 'Refunded' },
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
        .query(PaymentsDocument, this.buildVariables(null), {
          requestPolicy: 'network-only',
        })
        .toPromise();

      if (result.error) {
        this._error.set(toAppError(result.error));
      } else if (result.data?.payments) {
        this._items.set(result.data.payments.items);
        this._hasMore.set(result.data.payments.hasMore);
        this._nextCursor.set(result.data.payments.nextCursor ?? null);
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
        .query(PaymentsDocument, this.buildVariables(cursor), {
          requestPolicy: 'network-only',
        })
        .toPromise();

      if (result.error) {
        this._error.set(toAppError(result.error));
      } else if (result.data?.payments) {
        this._items.update((current) => [
          ...current,
          ...result.data!.payments.items,
        ]);
        this._hasMore.set(result.data.payments.hasMore);
        this._nextCursor.set(result.data.payments.nextCursor ?? null);
      }
    } catch (e) {
      this._error.set(toAppError(e));
    } finally {
      this._loadingMore.set(false);
    }
  }

  onStatusChange(value: string): void {
    this._status.set(value as PaymentStatus | '');
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

  statusClass(status: PaymentRow['status']): string {
    switch (status) {
      case 'SUCCEEDED':
        return 'payments__status--succeeded';
      case 'FAILED':
      case 'CANCELLED':
        return 'payments__status--failed';
      case 'PENDING':
      case 'PROCESSING':
        return 'payments__status--pending';
      case 'REFUNDED':
        return 'payments__status--refunded';
      default:
        return '';
    }
  }

  private buildVariables(after: string | null): PaymentsQueryVariables {
    const status = this._status();
    return {
      filter: status ? { status } : null,
      limit: this.pageSize,
      after,
    };
  }
}
