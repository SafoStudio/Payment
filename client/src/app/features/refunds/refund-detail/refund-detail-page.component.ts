import {
  ChangeDetectionStrategy,
  Component,
  inject,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-refund-detail-page',
  standalone: true,
  templateUrl: './refund-detail-page.component.html',
  styleUrl: './refund-detail-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RefundDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly refundId = this.route.snapshot.paramMap.get('id') ?? '';

  back(): void {
    void this.router.navigate(['/refunds']);
  }
}
