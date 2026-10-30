import {
  ChangeDetectionStrategy,
  Component,
  type OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { gqlClient } from '@core/graphql/client';
import {
  BalanceHistoryDocument,
  BlockWalletDocument,
  CloseWalletDocument,
  UnblockWalletDocument,
  WalletDocument,
  type BalanceHistoryQuery,
  type WalletQuery,
} from '@core/graphql/graphql';
import { toAppError, type AppError } from '@core/graphql/error-handler';
import { type DocumentNode } from 'graphql';
import { AuthService } from '@core/auth/auth.service';

type Wallet = WalletQuery['wallet'];
type HistoryRow = NonNullable<BalanceHistoryQuery['balanceHistory']['items']>[number];

@Component({
  selector: 'app-wallet-detail-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './wallet-detail-page.component.html',
  styleUrl: './wallet-detail-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WalletDetailPageComponent implements OnInit {
  private readonly pageSize = 20;
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly walletId = this.route.snapshot.paramMap.get('id') ?? '';

  private readonly _wallet = signal<Wallet | null>(null);
  private readonly _walletLoading = signal(true);
  private readonly _walletError = signal<AppError | null>(null);

  private readonly _history = signal<readonly HistoryRow[]>([]);
  private readonly _historyLoading = signal(true);
  private readonly _historyLoadingMore = signal(false);
  private readonly _historyError = signal<AppError | null>(null);
  private readonly _historyHasMore = signal(false);
  private readonly _historyCursor = signal<string | null>(null);

  private readonly _statusBusy = signal(false);
  private readonly _statusError = signal<string | null>(null);

  readonly wallet = this._wallet.asReadonly();
  readonly walletLoading = this._walletLoading.asReadonly();
  readonly walletError = this._walletError.asReadonly();

  readonly history = this._history.asReadonly();
  readonly historyLoading = this._historyLoading.asReadonly();
  readonly historyLoadingMore = this._historyLoadingMore.asReadonly();
  readonly historyError = this._historyError.asReadonly();
  readonly historyHasMore = this._historyHasMore.asReadonly();

  readonly statusBusy = this._statusBusy.asReadonly();
  readonly statusError = this._statusError.asReadonly();

  readonly canManage = computed(() => this.auth.hasPermission('wallet:block'));
  readonly isActive = computed(() => this._wallet()?.status === 'ACTIVE');
  readonly isBlocked = computed(() => this._wallet()?.status === 'BLOCKED');
  readonly isClosed = computed(() => this._wallet()?.status === 'CLOSED');

  readonly historyEmpty = computed(
    () =>
      !this._historyLoading() &&
      !this._historyError() &&
      this._history().length === 0,
  );

  ngOnInit(): void {
    void this.reloadWallet();
    void this.reloadHistory();
  }

  async reloadWallet(): Promise<void> {
    this._walletLoading.set(true);
    this._walletError.set(null);
    try {
      const result = await gqlClient
        .query(WalletDocument, { id: this.walletId }, { requestPolicy: 'network-only' })
        .toPromise();
      if (result.error) {
        this._walletError.set(toAppError(result.error));
      } else if (result.data?.wallet) {
        this._wallet.set(result.data.wallet);
      }
    } catch (e) {
      this._walletError.set(toAppError(e));
    } finally {
      this._walletLoading.set(false);
    }
  }

  async reloadHistory(): Promise<void> {
    this._historyLoading.set(true);
    this._historyError.set(null);
    this._history.set([]);
    this._historyHasMore.set(false);
    this._historyCursor.set(null);
    try {
      const result = await gqlClient
        .query(
          BalanceHistoryDocument,
          { walletId: this.walletId, limit: this.pageSize, after: null },
          { requestPolicy: 'network-only' },
        )
        .toPromise();
      if (result.error) {
        this._historyError.set(toAppError(result.error));
      } else if (result.data?.balanceHistory) {
        this._history.set(result.data.balanceHistory.items);
        this._historyHasMore.set(result.data.balanceHistory.hasMore);
        this._historyCursor.set(result.data.balanceHistory.nextCursor ?? null);
      }
    } catch (e) {
      this._historyError.set(toAppError(e));
    } finally {
      this._historyLoading.set(false);
    }
  }

  async loadMoreHistory(): Promise<void> {
    const cursor = this._historyCursor();
    if (!cursor || this._historyLoadingMore() || this._historyLoading()) return;
    this._historyLoadingMore.set(true);
    try {
      const result = await gqlClient
        .query(
          BalanceHistoryDocument,
          { walletId: this.walletId, limit: this.pageSize, after: cursor },
          { requestPolicy: 'network-only' },
        )
        .toPromise();
      if (result.error) {
        this._historyError.set(toAppError(result.error));
      } else if (result.data?.balanceHistory) {
        this._history.update((cur) => [...cur, ...result.data!.balanceHistory.items]);
        this._historyHasMore.set(result.data.balanceHistory.hasMore);
        this._historyCursor.set(result.data.balanceHistory.nextCursor ?? null);
      }
    } catch (e) {
      this._historyError.set(toAppError(e));
    } finally {
      this._historyLoadingMore.set(false);
    }
  }

  async changeStatus(kind: 'block' | 'unblock' | 'close'): Promise<void> {
    if (this._statusBusy()) return;
    this._statusBusy.set(true);
    this._statusError.set(null);
    try {
      let doc: DocumentNode;
      switch (kind) {
        case 'block':
          doc = BlockWalletDocument;
          break;
        case 'unblock':
          doc = UnblockWalletDocument;
          break;
        case 'close':
          doc = CloseWalletDocument;
          break;
      }

      const result = await gqlClient
        .mutation(doc, { id: this.walletId })
        .toPromise();

      if (result.error) {
        this._statusError.set(toAppError(result.error).message);
      } else {
        await this.reloadWallet();
      }
    } catch (e) {
      this._statusError.set(toAppError(e).message);
    } finally {
      this._statusBusy.set(false);
    }
  }

  formatAmount(amount: string, currency: string): string {
    const value = Number(amount);
    if (!Number.isFinite(value)) return amount;
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency,
    }).format(value);
  }

  formatDelta(amount: string, currency: string): string {
    const value = Number(amount);
    if (!Number.isFinite(value)) return amount;
    const formatted = new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency,
    }).format(Math.abs(value));
    return value >= 0 ? `+${formatted}` : `-${formatted}`;
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

  operationLabel(type: string): string {
    switch (type) {
      case 'TRANSFER_DEBIT':
        return 'Transfer out';
      case 'TRANSFER_CREDIT':
        return 'Transfer in';
      case 'TOP_UP_CREDIT':
        return 'Top up';
      case 'FEE_DEBIT':
        return 'Fee charged';
      case 'FEE_CREDIT':
        return 'Fee received';
      default:
        return type;
    }
  }

  operationClass(type: string): string {
    return type.endsWith('_DEBIT')
      ? 'wallet-detail__delta--debit'
      : 'wallet-detail__delta--credit';
  }

  statusClass(status: string): string {
    switch (status) {
      case 'ACTIVE':
        return 'wallet-detail__status--active';
      case 'BLOCKED':
        return 'wallet-detail__status--blocked';
      case 'CLOSED':
        return 'wallet-detail__status--closed';
      default:
        return '';
    }
  }

  goBack(): void {
    void this.router.navigate(['/wallets']);
  }
}
