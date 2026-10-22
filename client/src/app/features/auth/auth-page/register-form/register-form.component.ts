import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@core/auth/auth.service';
import { ButtonComponent } from '@shared/ui/button/button.component';
import { InputComponent } from '@shared/ui/input/input.component';
import { FormFieldComponent } from '@shared/ui/form-field/form-field.component';
import { toErrorMessage } from '@shared/utils/error';


interface RegisterResponse {
  userId: string;
  email: string;
  passkeyEnrollmentRequired: boolean;
}

@Component({
  selector: 'app-register-form',
  imports: [ButtonComponent, InputComponent, FormFieldComponent],
  templateUrl: './register-form.component.html',
  styleUrl: './register-form.component.scss',
})
export class RegisterFormComponent {
  private readonly auth: AuthService = inject(AuthService);
  private readonly router: Router = inject(Router);

  protected readonly email = signal('');
  protected readonly firstName = signal('');
  protected readonly lastName = signal('');
  protected readonly password = signal('');
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected onEmailChange(v: string): void { this.email.set(v); }
  protected onFirstNameChange(v: string): void { this.firstName.set(v); }
  protected onLastNameChange(v: string): void { this.lastName.set(v); }
  protected onPasswordChange(v: string): void { this.password.set(v); }

  protected async onSubmit(event: Event): Promise<void> {
    event.preventDefault();
    if (this.loading()) return;

    this.loading.set(true);
    this.error.set(null);

    try {
      const response = await fetch('/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          email: this.email(),
          firstName: this.firstName(),
          lastName: this.lastName(),
          password: this.password(),
        }),
      });

      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        const message = typeof body?.error === 'string' ? body.error : 'Registration failed';
        throw new Error(message);
      }

      await response.json() as RegisterResponse;

      await this.auth.loadSession();

      const user = this.auth.user();
      if (user && !user.hasPasskey) {
        await this.router.navigate(['/passkey-enrollment']);
      } else {
        await this.router.navigate(['/dashboard']);
      }
    } catch (e) {
      this.error.set(toErrorMessage(e, 'Registration failed'));
    } finally {
      this.loading.set(false);
    }
  }
}
