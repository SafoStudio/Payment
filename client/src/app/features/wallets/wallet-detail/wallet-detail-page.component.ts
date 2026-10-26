import {
  ChangeDetectionStrategy,
  Component,
  inject,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-wallet-detail-page',
  standalone: true,
  templateUrl: './wallet-detail-page.component.html',
  styleUrl: './wallet-detail-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WalletDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly walletId = this.route.snapshot.paramMap.get('id') ?? '';

  back(): void {
    void this.router.navigate(['/wallets']);
  }
}
