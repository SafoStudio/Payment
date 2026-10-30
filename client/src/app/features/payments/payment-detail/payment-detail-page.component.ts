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
  CancelPaymentDocument,
  CreateRefundDocument,
  PaymentDetailDocument,
  PaymentRefundsDocument,
  ProcessPaymentDocument,
  type CreateRefundInput,
  type PaymentDetailQuery,
  type PaymentRefundsQuery,
} from '@core/graphql/graphql';
import { toAppError, type AppError } from '@core/graphql/error-handler';
import { AuthService } from '@core/auth/auth.service';

type Payment = PaymentDetailQuery['payment'];
type RefundRow = NonNullable<PaymentRefundsQuery['refundsByPayment']>[number];

@Component({
  selector: 'app-payment-detail-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './payment-detail-page.component.html',
  styleUrl: './payment-detail-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentDetailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly paymentId = this.route.snapshot.paramMap.get('id') ?? '';

  private readonly _payment = signal<Payment | null>(null);
  private readonly _refunds = signal<readonly RefundRow[]>([]);
  private readonly _loading = signal(true);
  private readonly _error = signal<AppError | null>(null);

  private readonly _actionBusy = signal(false);
  private readonly _actionError = signal<string | null>(null);

  private readonly _refundFormOpen = signal(false);
  private readonly _refundAmount = signal('');
  private readonly _refundReason = signal('');
  private readonly _refundError = signal<string | null>(null);

  readonly payment = this._payment.asReadonly();
  readonly refunds = this._refunds.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  readonly actionBusy = this._actionBusy.asReadonly();
  readonly actionError = this._actionError.asReadonly();

  readonly refundFormOpen = this._refundFormOpen.asReadonly();
  readonly refundAmount = this._refundAmount.asReadonly();
  readonly refundReason = this._refundReason.asReadonly();
  readonly refundError = this._refundError.asReadonly();

  readonly canManage = computed(() => this.auth.hasPermission('payment:manage:own'));
  readonly canRefund = computed(() => this.auth.hasPermission('refund:create'));

  readonly canProcess = computed(
    () => this.canManage() && this._payment()?.status === 'PENDING',
  );
  readonly canCancel = computed(() => {
    const status = this._payment()?.status;
    return this.canManage() && (status === 'PENDING' || status === 'PROCESSING');
  });
  readonly canCreateRefund = computed(
    () => this.canRefund() && this._payment()?.status === 'SUCCEEDED',
  );

  readonly refundsEmpty = computed(
    () => !this._loading() && !this._error() && this._refunds().length === 0,
  );

  ngOnInit(): void {
    void this.load();
  }

  async load(): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const [paymentResult, refundsResult] = await Promise.all([
        gqlClient
          .query(PaymentDetailDocument, { id: this.paymentId }, { requestPolicy: 'network-only' })
          .toPromise(),
        gqlClient
          .query(PaymentRefundsDocument, { paymentId: this.paymentId }, { requestPolicy: 'network-only' })
          .toPromise(),
      ]);

      if (paymentResult.error) {
        this._error.set(toAppError(paymentResult.error));
        return;
      }
      if (refundsResult.error) {
        this._error.set(toAppError(refundsResult.error));
        return;
      }

      if (paymentResult.data?.payment) {
        this._payment.set(paymentResult.data.payment);
      }
      if (refundsResult.data?.refundsByPayment) {
        this._refunds.set(refundsResult.data.refundsByPayment);
      }
    } catch (e) {
      this._error.set(toAppError(e));
    } finally {
      this._loading.set(false);
    }
  }

  async reloadPayment(): Promise<void> {
    try {
      const result = await gqlClient
        .query(PaymentDetailDocument, { id: this.paymentId }, { requestPolicy: 'network-only' })
        .toPromise();
      if (result.error) {
        this._actionError.set(toAppError(result.error).message);
      } else if (result.data?.payment) {
        this._payment.set(result.data.payment);
      }
    } catch (e) {
      this._actionError.set(toAppError(e).message);
    }
  }

  async reloadRefunds(): Promise<void> {
    try {
      const result = await gqlClient
        .query(PaymentRefundsDocument, { paymentId: this.paymentId }, { requestPolicy: 'network-only' })
        .toPromise();
      if (result.error) {
        this._actionError.set(toAppError(result.error).message);
      } else if (result.data?.refundsByPayment) {
        this._refunds.set(result.data.refundsByPayment);
      }
    } catch (e) {
      this._actionError.set(toAppError(e).message);
    }
  }

  async process(): Promise<void> {
    if (this._actionBusy()) return;
    this._actionBusy.set(true);
    this._actionError.set(null);
    try {
      const result = await gqlClient
        .mutation(ProcessPaymentDocument, { id: this.paymentId })
        .toPromise();
      if (result.error) {
        this._actionError.set(toAppError(result.error).message);
      } else {
        await this.reloadPayment();
      }
    } catch (e) {
      this._actionError.set(toAppError(e).message);
    } finally {
      this._actionBusy.set(false);
    }
  }

  async cancel(): Promise<void> {
    if (this._actionBusy()) return;
    this._actionBusy.set(true);
    this._actionError.set(null);
    try {
      const result = await gqlClient
        .mutation(CancelPaymentDocument, { id: this.paymentId })
        .toPromise();
      if (result.error) {
        this._actionError.set(toAppError(result.error).message);
      } else {
        await this.reloadPayment();
      }
    } catch (e) {
      this._actionError.set(toAppError(e).message);
    } finally {
      this._actionBusy.set(false);
    }
  }

  openRefundForm(): void {
    const payment = this._payment();
    if (!payment) return;
    this._refundFormOpen.set(true);
    this._refundAmount.set(payment.amount);
    this._refundReason.set('');
    this._refundError.set(null);
  }

  closeRefundForm(): void {
    if (this._actionBusy()) return;
    this._refundFormOpen.set(false);
    this._refundError.set(null);
  }

  onRefundAmountInput(value: string): void {
    this._refundAmount.set(value);
  }

  onRefundReasonInput(value: string): void {
    this._refundReason.set(value);
  }

  async submitRefund(): Promise<void> {
    const payment = this._payment();
    if (!payment || this._actionBusy()) return;

    const raw = this._refundAmount().trim().replace(',', '.');
    if (!raw || !/^\d+(\.\d{1,4})?$/.test(raw)) {
      this._refundError.set('Enter a positive amount with up to 4 decimals.');
      return;
    }
    const amount = Number(raw);
    if (amount <= 0) {
      this._refundError.set('Amount must be greater than zero.');
      return;
    }
    if (amount > Number(payment.amount)) {
      this._refundError.set('Refund amount cannot exceed the payment amount.');
      return;
    }

    const reason = this._refundReason().trim();
    if (reason.length > 500) {
      this._refundError.set('Reason is too long (max 500 characters).');
      return;
    }

    const input: CreateRefundInput = {
      paymentId: payment.id,
      amount: raw,
      currency: payment.currency,
      reason: reason || null,
      idempotencyKey: crypto.randomUUID(),
    };

    this._actionBusy.set(true);
    this._refundError.set(null);
    this._actionError.set(null);
    try {
      const result = await gqlClient
        .mutation(CreateRefundDocument, { input })
        .toPromise();
      if (result.error) {
        this._refundError.set(toAppError(result.error).message);
      } else {
        this._refundFormOpen.set(false);
        this._refundAmount.set('');
        this._refundReason.set('');
        await Promise.all([this.reloadPayment(), this.reloadRefunds()]);
      }
    } catch (e) {
      this._refundError.set(toAppError(e).message);
    } finally {
      this._actionBusy.set(false);
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
      case 'SUCCEEDED':
        return 'payment-detail__status--succeeded';
      case 'PENDING':
        return 'payment-detail__status--pending';
      case 'PROCESSING':
        return 'payment-detail__status--processing';
      case 'FAILED':
        return 'payment-detail__status--failed';
      case 'REFUNDED':
        return 'payment-detail__status--refunded';
      case 'CANCELLED':
        return 'payment-detail__status--cancelled';
      default:
        return '';
    }
  }

  refundStatusClass(status: string): string {
    switch (status) {
      case 'COMPLETED':
        return 'payment-detail__status--succeeded';
      case 'PENDING':
      case 'PROCESSING':
        return 'payment-detail__status--pending';
      case 'FAILED':
      case 'REJECTED':
        return 'payment-detail__status--failed';
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
      case 'PAYMENT':
        return 'Payment';
      default:
        return type;
    }
  }

  back(): void {
    void this.router.navigate(['/payments']);
  }
}
