import { Component, signal } from '@angular/core';
import { LoginFormComponent } from './login-form/login-form.component';
import { RegisterFormComponent } from './register-form/register-form.component';

export type AuthMode = 'login' | 'register';

@Component({
  selector: 'app-auth-page',
  imports: [LoginFormComponent, RegisterFormComponent],
  templateUrl: './auth-page.component.html',
  styleUrl: './auth-page.component.scss',
})
export class AuthPageComponent {
  protected readonly mode = signal<AuthMode>('login');

  protected setMode(mode: AuthMode): void {
    this.mode.set(mode);
  }
}
