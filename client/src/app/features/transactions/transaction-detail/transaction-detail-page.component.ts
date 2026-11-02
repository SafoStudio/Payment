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
  TransactionDetailDocument,
  TransactionFeesDocument,
  type TransactionDetailQuery,
  type TransactionFeesQuery,
} from '@core/graphql/graphql';
import { toAppError, type AppError } from '@core/graphql/error-handler';
import { AuthService } from '@core/auth/auth.service';

type Transaction = TransactionDetailQuery['transaction'];
type FeeRow = NonNullable<TransactionFeesQuery['feesByTransaction']>[number];

@Component({
  selector: 'app-transaction-detail-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './transaction-detail-page.component.html',
  styleUrl: './transaction-detail-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TransactionDetailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly transactionId = this.route.snapshot.paramMap.get('id') ?? '';

  private readonly _transaction = signal<Transaction | null>(null);
  private readonly _fees = signal<readonly FeeRow[]>([]);
  private readonly _loading = signal(true);
  private readonly _error = signal<AppError | null>(null);

  readonly transaction = this._transaction.asReadonly();
  readonly fees = this._fees.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  readonly canReadAllWallets = computed(() =>
    this.auth.hasAnyPermission(['wallet:read:all']),
  );

  ngOnInit(): void {
    void this.load();
  }

  async load(): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const [txResult, feesResult] = await Promise.all([
        gqlClient
          .query(
            TransactionDetailDocument,
            { id: this.transactionId },
            { requestPolicy: 'network-only' },
          )
          .toPromise(),
        gqlClient
          .query(
            TransactionFeesDocument,
            { transactionId: this.transactionId },
            { requestPolicy: 'network-only' },
          )
          .toPromise(),
      ]);

      if (txResult.error) {
        this._error.set(toAppError(txResult.error));
        return;
      }
      if (feesResult.error) {
        this._error.set(toAppError(feesResult.error));
        return;
      }

      if (txResult.data?.transaction) {
        this._transaction.set(txResult.data.transaction);
      }
      if (feesResult.data?.feesByTransaction) {
        this._fees.set(feesResult.data.feesByTransaction);
      }
    } catch (e) {
      this._error.set(toAppError(e));
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Builds a router link for the business entity that produced this ledger
   * transaction, based on (referenceType, referenceId). Returns null when
   * there is no navigable reference (ADJUSTMENT/EXTERNAL, or missing pair).
   */
  referenceLink(
    referenceType: string | null,
    referenceId: string | null,
  ): (string | number)[] | null {
    if (!referenceType || !referenceId) return null;
    switch (referenceType) {
      case 'PAYMENT':
        return ['/payments', referenceId];
      case 'REFUND':
        return ['/refunds', referenceId];
      default:
        return null;
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

  formatDate(iso: string): string {
    return new Date(iso).toLocaleString(undefined, {
      year: 'numeric',
      month: 'short',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  statusClass(status: string): string {
    switch (status) {
      case 'COMPLETED':
        return 'transaction-detail__status--completed';
      case 'PENDING':
        return 'transaction-detail__status--pending';
      case 'FAILED':
        return 'transaction-detail__status--failed';
      case 'CANCELLED':
        return 'transaction-detail__status--cancelled';
      default:
        return '';
    }
  }

  typeLabel(type: string): string {
    switch (type) {
      case 'TRANSFER':
        return 'Transfer';
      case 'TOP_UP':
        return 'Top up';
      case 'WITHDRAWAL':
        return 'Withdrawal';
      case 'REFUND':
        return 'Refund';
      case 'ADJUSTMENT':
        return 'Adjustment';
      default:
        return type;
    }
  }

  referenceTypeLabel(referenceType: string | null): string {
    if (!referenceType) return '';
    switch (referenceType) {
      case 'PAYMENT':
        return 'Payment';
      case 'REFUND':
        return 'Refund';
      case 'ADJUSTMENT':
        return 'Adjustment';
      case 'EXTERNAL':
        return 'External';
      default:
        return referenceType;
    }
  }

  feeTypeLabel(type: string): string {
    switch (type) {
      case 'FIXED':
        return 'Fixed';
      case 'PERCENTAGE':
        return 'Percentage';
      case 'MIXED':
        return 'Mixed';
      case 'TIERED':
        return 'Tiered';
      default:
        return type;
    }
  }

  back(): void {
    void this.router.navigate(['/transactions']);
  }
}
