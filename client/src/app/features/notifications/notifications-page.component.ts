import {
  ChangeDetectionStrategy,
  Component,
  type OnInit,
  computed,
  signal,
} from '@angular/core';
import { gqlClient } from '@core/graphql/client';
import {
  NotificationsDocument,
  type NotificationStatus,
  type NotificationType,
  type NotificationsQuery,
  type NotificationsQueryVariables,
} from '@core/graphql/graphql';
import { toAppError, type AppError } from '@core/graphql/error-handler';
import { formatDate } from '@shared/utils/date';

type NotificationRow = NonNullable<NotificationsQuery['notifications']['items']>[number];

interface StatusOption {
  value: NotificationStatus | '';
  label: string;
}

interface TypeOption {
  value: NotificationType | '';
  label: string;
}

@Component({
  selector: 'app-notifications-page',
  standalone: true,
  imports: [],
  templateUrl: './notifications-page.component.html',
  styleUrl: './notifications-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationsPageComponent implements OnInit {
  private readonly pageSize = 20;

  private readonly _loading = signal(true);
  private readonly _loadingMore = signal(false);
  private readonly _error = signal<AppError | null>(null);
  private readonly _items = signal<readonly NotificationRow[]>([]);
  private readonly _hasMore = signal(false);
  private readonly _nextCursor = signal<string | null>(null);
  private readonly _status = signal<NotificationStatus | ''>('');
  private readonly _type = signal<NotificationType | ''>('');

  readonly loading = this._loading.asReadonly();
  readonly loadingMore = this._loadingMore.asReadonly();
  readonly error = this._error.asReadonly();
  readonly items = this._items.asReadonly();
  readonly hasMore = this._hasMore.asReadonly();
  readonly status = this._status.asReadonly();
  readonly type = this._type.asReadonly();

  readonly isEmpty = computed(
    () => !this._loading() && !this._error() && this._items().length === 0,
  );

  readonly statusOptions: readonly StatusOption[] = [
    { value: '', label: 'All statuses' },
    { value: 'PENDING', label: 'Pending' },
    { value: 'SENT', label: 'Sent' },
    { value: 'FAILED', label: 'Failed' },
    { value: 'CANCELLED', label: 'Cancelled' },
  ];

  readonly typeOptions: readonly TypeOption[] = [
    { value: '', label: 'All types' },
    { value: 'PAYMENT_RECEIVED', label: 'Payment received' },
    { value: 'PAYMENT_FAILED', label: 'Payment failed' },
    { value: 'REFUND_PROCESSED', label: 'Refund processed' },
    { value: 'REFUND_FAILED', label: 'Refund failed' },
  ];

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
        .query<NotificationsQuery, NotificationsQueryVariables>(
          NotificationsDocument,
          this.buildVariables(null),
          { requestPolicy: 'network-only' },
        )
        .toPromise();

      if (result.error) {
        this._error.set(toAppError(result.error));
      } else if (result.data?.notifications) {
        this._items.set(result.data.notifications.items);
        this._hasMore.set(result.data.notifications.hasMore);
        this._nextCursor.set(result.data.notifications.nextCursor ?? null);
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
        .query<NotificationsQuery, NotificationsQueryVariables>(
          NotificationsDocument,
          this.buildVariables(cursor),
          { requestPolicy: 'network-only' },
        )
        .toPromise();

      if (result.error) {
        this._error.set(toAppError(result.error));
      } else if (result.data?.notifications) {
        this._items.update((current) => [...current, ...result.data!.notifications.items]);
        this._hasMore.set(result.data.notifications.hasMore);
        this._nextCursor.set(result.data.notifications.nextCursor ?? null);
      }
    } catch (e) {
      this._error.set(toAppError(e));
    } finally {
      this._loadingMore.set(false);
    }
  }

  onStatusChange(value: string): void {
    this._status.set(value as NotificationStatus | '');
    void this.reload();
  }

  onTypeChange(value: string): void {
    this._type.set(value as NotificationType | '');
    void this.reload();
  }

  statusClass(status: string): string {
    switch (status) {
      case 'SENT':
        return 'status--succeeded';
      case 'PENDING':
        return 'status--pending';
      case 'FAILED':
        return 'status--failed';
      case 'CANCELLED':
        return 'status--cancelled';
      default:
        return '';
    }
  }

  private buildVariables(after: string | null): NotificationsQueryVariables {
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
