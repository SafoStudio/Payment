import {
  ChangeDetectionStrategy,
  Component,
  type OnInit,
  computed,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { gqlClient } from '@core/graphql/client';
import { WalletsDocument, type WalletsQuery } from '@core/graphql/graphql';
import { toAppError, type AppError } from '@core/graphql/error-handler';

type WalletRow = NonNullable<WalletsQuery['wallets']>[number];

@Component({
  selector: 'app-wallets-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './wallets-page.component.html',
  styleUrl: './wallets-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WalletsPageComponent implements OnInit {
  private readonly _loading = signal(true);
  private readonly _error = signal<AppError | null>(null);
  private readonly _wallets = signal<readonly WalletRow[]>([]);

  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly wallets = this._wallets.asReadonly();

  readonly isEmpty = computed(
    () => !this._loading() && !this._error() && this._wallets().length === 0,
  );

  ngOnInit(): void {
    void this.load();
  }

  async load(): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const result = await gqlClient
        .query(WalletsDocument, {}, { requestPolicy: 'network-only' })
        .toPromise();
      if (result.error) {
        this._error.set(toAppError(result.error));
        this._wallets.set([]);
      } else {
        this._wallets.set(result.data?.wallets ?? []);
      }
    } catch (e) {
      this._error.set(toAppError(e));
      this._wallets.set([]);
    } finally {
      this._loading.set(false);
    }
  }

  formatBalance(balance: string, currency: string): string {
    const amount = Number(balance);
    if (!Number.isFinite(amount)) return balance;
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency,
    }).format(amount);
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

  statusClass(status: WalletRow['status']): string {
    switch (status) {
      case 'ACTIVE':
        return 'wallets__status--active';
      case 'BLOCKED':
        return 'wallets__status--blocked';
      case 'CLOSED':
        return 'wallets__status--closed';
      default:
        return '';
    }
  }
}
