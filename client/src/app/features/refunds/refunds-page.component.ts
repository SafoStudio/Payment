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
  RefundsDocument,
  type RefundStatus,
  type RefundsQuery,
  type RefundsQueryVariables,
} from '@core/graphql/graphql';
import { toAppError, type AppError } from '@core/graphql/error-handler';

type RefundRow = NonNullable<RefundsQuery['refunds']['items']>[number];

interface StatusOption {
  value: RefundStatus | '';
  label: string;
}

@Component({
  selector: 'app-refunds-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './refunds-page.component.html',
  styleUrl: './refunds-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RefundsPageComponent implements OnInit {
  private readonly pageSize = 20;

  private readonly _loading = signal(true);
  private readonly _loadingMore = signal(false);
  private readonly _error = signal<AppError | null>(null);
  private readonly _items = signal<readonly RefundRow[]>([]);
  private readonly _hasMore = signal(false);
  private readonly _nextCursor = signal<string | null>(null);
  private readonly _status = signal<RefundStatus | ''>('');

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
    { value: 'COMPLETED', label: 'Completed' },
    { value: 'FAILED', label: 'Failed' },
    { value: 'REJECTED', label: 'Rejected' },
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
        .query(RefundsDocument, this.buildVariables(null), {
          requestPolicy: 'network-only',
        })
        .toPromise();

      if (result.error) {
        this._error.set(toAppError(result.error));
      } else if (result.data?.refunds) {
        this._items.set(result.data.refunds.items);
        this._hasMore.set(result.data.refunds.hasMore);
        this._nextCursor.set(result.data.refunds.nextCursor ?? null);
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
        .query(RefundsDocument, this.buildVariables(cursor), {
          requestPolicy: 'network-only',
        })
        .toPromise();

      if (result.error) {
        this._error.set(toAppError(result.error));
      } else if (result.data?.refunds) {
        this._items.update((current) => [
          ...current,
          ...result.data!.refunds.items,
        ]);
        this._hasMore.set(result.data.refunds.hasMore);
        this._nextCursor.set(result.data.refunds.nextCursor ?? null);
      }
    } catch (e) {
      this._error.set(toAppError(e));
    } finally {
      this._loadingMore.set(false);
    }
  }

  onStatusChange(value: string): void {
    this._status.set(value as RefundStatus | '');
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

  statusClass(status: RefundRow['status']): string {
    switch (status) {
      case 'COMPLETED':
        return 'refunds__status--completed';
      case 'FAILED':
      case 'REJECTED':
        return 'refunds__status--failed';
      case 'PENDING':
      case 'PROCESSING':
        return 'refunds__status--pending';
      default:
        return '';
    }
  }

  private buildVariables(after: string | null): RefundsQueryVariables {
    const status = this._status();
    return {
      filter: status ? { status } : null,
      limit: this.pageSize,
      after,
    };
  }
}
