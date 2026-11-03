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
import { walletStatusClass } from '@shared/utils/status';
import { formatAmount } from '@shared/utils/money';
import { formatDate } from '@shared/utils/date';

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

  protected readonly formatAmount = formatAmount;
  protected readonly formatDate = formatDate;

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

  statusClass(status: WalletRow['status']): string {
    return walletStatusClass(status);
  }
}
