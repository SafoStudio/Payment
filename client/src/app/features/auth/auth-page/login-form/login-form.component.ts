import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@core/auth/auth.service';
import { ButtonComponent } from '@shared/ui/button/button.component';
import { InputComponent } from '@shared/ui/input/input.component';
import { FormFieldComponent } from '@shared/ui/form-field/form-field.component';
import { toErrorMessage } from '@shared/utils/error';

@Component({
  selector: 'app-login-form',
  imports: [ButtonComponent, InputComponent, FormFieldComponent],
  templateUrl: './login-form.component.html',
  styleUrl: './login-form.component.scss',
})
export class LoginFormComponent {
  private readonly auth: AuthService = inject(AuthService);
  private readonly router: Router = inject(Router);

  protected readonly email = signal('');
  protected readonly password = signal('');
  protected readonly loading = signal(false);
  protected readonly passkeyLoading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected onEmailChange(value: string): void {
    this.email.set(value);
  }

  protected onPasswordChange(value: string): void {
    this.password.set(value);
  }

  protected async onPasskeyLogin(): Promise<void> {
    if (this.passkeyLoading()) return;
    if (!this.email()) {
      this.error.set('Enter your email first');
      return;
    }

    this.passkeyLoading.set(true);
    this.error.set(null);

    try {
      await this.auth.loginWithPasskey(this.email());
      await this.router.navigate(['/dashboard']);
    } catch (e) {
      this.error.set(toErrorMessage(e, 'Passkey login failed'));
    } finally {
      this.passkeyLoading.set(false);
    }
  }

  protected async onSubmit(event: Event): Promise<void> {
    event.preventDefault();
    if (this.loading()) return;

    this.loading.set(true);
    this.error.set(null);

    try {
      await this.auth.loginWithPassword(this.email(), this.password());
      await this.router.navigate(['/dashboard']);
    } catch (e) {
      this.error.set(toErrorMessage(e, 'Login failed'));
    } finally {
      this.loading.set(false);
    }
  }
}
