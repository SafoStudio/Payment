import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@core/auth/auth.service';

@Component({
  selector: 'app-user-menu',
  standalone: true,
  templateUrl: './user-menu.component.html',
  styleUrl: './user-menu.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserMenuComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly user = this.auth.user;
  readonly busy = signal(false);

  async logout(): Promise<void> {
    if (this.busy()) return;
    this.busy.set(true);
    try {
      await this.auth.logout();
      await this.router.navigate(['/auth']);
    } finally {
      this.busy.set(false);
    }
  }
}
