import {
  ChangeDetectionStrategy,
  Component,
  inject,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-payment-detail-page',
  standalone: true,
  templateUrl: './payment-detail-page.component.html',
  styleUrl: './payment-detail-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly paymentId = this.route.snapshot.paramMap.get('id') ?? '';

  back(): void {
    void this.router.navigate(['/payments']);
  }
}
