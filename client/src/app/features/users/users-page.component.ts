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
  UsersDocument,
  type UserStatus,
  type UsersQuery,
  type UsersQueryVariables,
} from '@core/graphql/graphql';
import { toAppError, type AppError } from '@core/graphql/error-handler';

type UserRow = NonNullable<UsersQuery['users']['items']>[number];

interface StatusOption {
  value: UserStatus | '';
  label: string;
}

interface RoleOption {
  value: string;
  label: string;
}

@Component({
  selector: 'app-users-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './users-page.component.html',
  styleUrl: './users-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UsersPageComponent implements OnInit {
  private readonly pageSize = 20;

  private readonly _loading = signal(true);
  private readonly _loadingMore = signal(false);
  private readonly _error = signal<AppError | null>(null);
  private readonly _items = signal<readonly UserRow[]>([]);
  private readonly _hasMore = signal(false);
  private readonly _nextCursor = signal<string | null>(null);
  private readonly _status = signal<UserStatus | ''>('');
  private readonly _roleCode = signal<string>('');

  readonly loading = this._loading.asReadonly();
  readonly loadingMore = this._loadingMore.asReadonly();
  readonly error = this._error.asReadonly();
  readonly items = this._items.asReadonly();
  readonly hasMore = this._hasMore.asReadonly();
  readonly status = this._status.asReadonly();
  readonly roleCode = this._roleCode.asReadonly();

  readonly statusOptions: readonly StatusOption[] = [
    { value: '', label: 'All statuses' },
    { value: 'ACTIVE', label: 'Active' },
    { value: 'BLOCKED', label: 'Blocked' },
    { value: 'DELETED', label: 'Deleted' },
  ];

  readonly roleOptions: readonly RoleOption[] = [
    { value: '', label: 'All roles' },
    { value: 'ADMIN', label: 'Admin' },
    { value: 'MERCHANT', label: 'Merchant' },
    { value: 'SUPPORT', label: 'Support' },
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
        .query(UsersDocument, this.buildVariables(null), {
          requestPolicy: 'network-only',
        })
        .toPromise();

      if (result.error) {
        this._error.set(toAppError(result.error));
      } else if (result.data?.users) {
        this._items.set(result.data.users.items);
        this._hasMore.set(result.data.users.hasMore);
        this._nextCursor.set(result.data.users.nextCursor ?? null);
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
        .query(UsersDocument, this.buildVariables(cursor), {
          requestPolicy: 'network-only',
        })
        .toPromise();

      if (result.error) {
        this._error.set(toAppError(result.error));
      } else if (result.data?.users) {
        this._items.update((current) => [
          ...current,
          ...result.data!.users.items,
        ]);
        this._hasMore.set(result.data.users.hasMore);
        this._nextCursor.set(result.data.users.nextCursor ?? null);
      }
    } catch (e) {
      this._error.set(toAppError(e));
    } finally {
      this._loadingMore.set(false);
    }
  }

  onStatusChange(value: string): void {
    this._status.set(value as UserStatus | '');
    void this.reload();
  }

  onRoleChange(value: string): void {
    this._roleCode.set(value);
    void this.reload();
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

  statusClass(status: UserRow['status']): string {
    switch (status) {
      case 'ACTIVE':
        return 'users__status--active';
      case 'BLOCKED':
        return 'users__status--blocked';
      case 'DELETED':
        return 'users__status--deleted';
      default:
        return '';
    }
  }

  private buildVariables(after: string | null): UsersQueryVariables {
    const status = this._status();
    const roleCode = this._roleCode();
    const filter =
      status || roleCode
        ? {
            ...(status ? { status } : {}),
            ...(roleCode ? { roleCode } : {}),
          }
        : null;

    return {
      filter,
      limit: this.pageSize,
      after,
    };
  }
}
