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
  AuditLogsDocument,
  type AuditLogsQuery,
  type AuditLogsQueryVariables,
} from '@core/graphql/graphql';
import { toAppError, type AppError } from '@core/graphql/error-handler';
import { formatDate } from '@shared/utils/date';

type AuditLogRow = NonNullable<AuditLogsQuery['auditLogs']['items']>[number];

interface EntityTypeOption {
  value: string;
  label: string;
}

interface ActionOption {
  value: string;
  label: string;
}

@Component({
  selector: 'app-audit-log-page',
  standalone: true,
  templateUrl: './audit-log-page.component.html',
  styleUrl: './audit-log-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuditLogPageComponent implements OnInit {
  private readonly pageSize = 20;

  private readonly _loading = signal(true);
  private readonly _loadingMore = signal(false);
  private readonly _error = signal<AppError | null>(null);
  private readonly _items = signal<readonly AuditLogRow[]>([]);
  private readonly _hasMore = signal(false);
  private readonly _nextCursor = signal<string | null>(null);

  private readonly _entityType = signal('');
  private readonly _action = signal('');
  private readonly _userId = signal('');

  private readonly _expandedId = signal<string | null>(null);

  readonly loading = this._loading.asReadonly();
  readonly loadingMore = this._loadingMore.asReadonly();
  readonly error = this._error.asReadonly();
  readonly items = this._items.asReadonly();
  readonly hasMore = this._hasMore.asReadonly();
  readonly entityType = this._entityType.asReadonly();
  readonly action = this._action.asReadonly();
  readonly userId = this._userId.asReadonly();
  readonly expandedId = this._expandedId.asReadonly();

  readonly entityTypeOptions: readonly EntityTypeOption[] = [
    { value: '', label: 'All entities' },
    { value: 'WALLET', label: 'Wallet' },
    { value: 'PAYMENT', label: 'Payment' },
    { value: 'REFUND', label: 'Refund' },
    { value: 'TRANSACTION', label: 'Transaction' },
    { value: 'USER', label: 'User' },
  ];

  readonly actionOptions: readonly ActionOption[] = [
    { value: '', label: 'All actions' },
    { value: 'ADJUST_BALANCE', label: 'Adjust balance' },
    { value: 'BLOCK', label: 'Block' },
    { value: 'UNBLOCK', label: 'Unblock' },
    { value: 'CLOSE', label: 'Close' },
    { value: 'UPDATE', label: 'Update' },
    { value: 'DELETE', label: 'Delete' },
  ];

  readonly isEmpty = computed(
    () => !this._loading() && !this._error() && this._items().length === 0,
  );

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
    this._expandedId.set(null);

    try {
      const result = await gqlClient
        .query(AuditLogsDocument, this.buildVariables(null), {
          requestPolicy: 'network-only',
        })
        .toPromise();

      if (result.error) {
        this._error.set(toAppError(result.error));
      } else if (result.data?.auditLogs) {
        this._items.set(result.data.auditLogs.items);
        this._hasMore.set(result.data.auditLogs.hasMore);
        this._nextCursor.set(result.data.auditLogs.nextCursor ?? null);
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
        .query(AuditLogsDocument, this.buildVariables(cursor), {
          requestPolicy: 'network-only',
        })
        .toPromise();

      if (result.error) {
        this._error.set(toAppError(result.error));
      } else if (result.data?.auditLogs) {
        this._items.update((current) => [
          ...current,
          ...result.data!.auditLogs.items,
        ]);
        this._hasMore.set(result.data.auditLogs.hasMore);
        this._nextCursor.set(result.data.auditLogs.nextCursor ?? null);
      }
    } catch (e) {
      this._error.set(toAppError(e));
    } finally {
      this._loadingMore.set(false);
    }
  }

  onEntityTypeChange(value: string): void {
    this._entityType.set(value);
    void this.reload();
  }

  onActionChange(value: string): void {
    this._action.set(value);
    void this.reload();
  }

  onUserIdInput(value: string): void {
    this._userId.set(value.trim());
  }

  applyUserIdFilter(): void {
    void this.reload();
  }

  toggleExpand(id: string): void {
    this._expandedId.update((cur) => (cur === id ? null : id));
  }

  prettyJson(raw: string | null): string {
    if (!raw) return '—';
    try {
      return JSON.stringify(JSON.parse(raw), null, 2);
    } catch {
      return raw;
    }
  }

  private buildVariables(after: string | null): AuditLogsQueryVariables {
    const entityType = this._entityType();
    const action = this._action();
    const userId = this._userId();
    return {
      entityType: entityType || null,
      action: action || null,
      userId: userId || null,
      limit: this.pageSize,
      after,
    };
  }
}
