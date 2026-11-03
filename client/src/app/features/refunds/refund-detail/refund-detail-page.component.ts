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
  ProcessRefundDocument,
  RefundDetailDocument,
  type RefundDetailQuery,
} from '@core/graphql/graphql';
import { toAppError, type AppError } from '@core/graphql/error-handler';
import { AuthService } from '@core/auth/auth.service';
import { refundStatusClass as refundStatusCssClass } from '@shared/utils/status';
import { formatAmount } from '@shared/utils/money';
import { formatDate } from '@shared/utils/date';

type Refund = RefundDetailQuery['refund'];

@Component({
  selector: 'app-refund-detail-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './refund-detail-page.component.html',
  styleUrl: './refund-detail-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RefundDetailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly refundId = this.route.snapshot.paramMap.get('id') ?? '';

  private readonly _refund = signal<Refund | null>(null);
  private readonly _loading = signal(true);
  private readonly _error = signal<AppError | null>(null);

  private readonly _actionBusy = signal(false);
  private readonly _actionError = signal<string | null>(null);

  readonly refund = this._refund.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly actionBusy = this._actionBusy.asReadonly();
  readonly actionError = this._actionError.asReadonly();

  readonly canProcessRefund = computed(() => {
    const status = this._refund()?.status;
    return this.auth.hasPermission('refund:process') && status === 'PENDING';
  });

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
        .query(
          RefundDetailDocument,
          { id: this.refundId },
          { requestPolicy: 'network-only' },
        )
        .toPromise();

      if (result.error) {
        this._error.set(toAppError(result.error));
        return;
      }
      if (result.data?.refund) {
        this._refund.set(result.data.refund);
      }
    } catch (e) {
      this._error.set(toAppError(e));
    } finally {
      this._loading.set(false);
    }
  }

  async processRefund(): Promise<void> {
    if (this._actionBusy()) return;
    this._actionBusy.set(true);
    this._actionError.set(null);
    try {
      const result = await gqlClient
        .mutation(ProcessRefundDocument, { id: this.refundId })
        .toPromise();

      if (result.error) {
        this._actionError.set(toAppError(result.error).message);
        return;
      }
      await this.load();
    } catch (e) {
      this._actionError.set(toAppError(e).message);
    } finally {
      this._actionBusy.set(false);
    }
  }

  statusClass(status: string): string {
    return refundStatusCssClass(status);
  }

  back(): void {
    void this.router.navigate(['/refunds']);
  }
}
