import {
  ChangeDetectionStrategy,
  Component,
  inject,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-transaction-detail-page',
  standalone: true,
  templateUrl: './transaction-detail-page.component.html',
  styleUrl: './transaction-detail-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TransactionDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly transactionId = this.route.snapshot.paramMap.get('id') ?? '';

  back(): void {
    void this.router.navigate(['/transactions']);
  }
}
