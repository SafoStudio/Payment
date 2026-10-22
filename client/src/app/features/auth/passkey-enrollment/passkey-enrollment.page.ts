import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@core/auth/auth.service';
import { ButtonComponent } from '@shared/ui/button/button.component';

@Component({
  selector: 'app-passkey-enrollment-page',
  imports: [ButtonComponent],
  template: `
    <div class="enrollment">
      <h1>Create a Passkey</h1>
      <p>Your account is ready. Create a passkey to sign in without a password next time.</p>
      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      }
      <app-button variant="primary" [fullWidth]="true" [loading]="loading()" (clicked)="enroll()">
        Create Passkey
      </app-button>
      <app-button variant="secondary" [fullWidth]="true" (clicked)="skip()">
        Skip for now
      </app-button>
    </div>
  `,
  styles: [`
    .enrollment {
      display: flex;
      flex-direction: column;
      gap: var(--space-md);
      max-width: 420px;
      margin: var(--space-2xl) auto;
      padding: var(--space-xl);
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-lg);
    }
    .error { color: var(--color-danger); }
  `],
})
export class PasskeyEnrollmentPage {
  private readonly auth: AuthService = inject(AuthService);
  private readonly router: Router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected async enroll(): Promise<void> {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set(null);

    try {
      await this.auth.enrollPasskey();
      await this.router.navigate(['/dashboard']);
    } catch (e) {
      const message = e instanceof Error ? e.message : 'Passkey registration failed';
      this.error.set(message);
    } finally {
      this.loading.set(false);
    }
  }

  protected async skip(): Promise<void> {
    await this.router.navigate(['/dashboard']);
  }
}
