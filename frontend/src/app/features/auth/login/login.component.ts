import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { environment as env } from '@environments/environment';
import { ModalComponent } from '@shared/components/modal/modal.component';

@Component({
  templateUrl: './login.component.html',
  imports: [ReactiveFormsModule, RouterLink, ModalComponent],
})
export class LoginComponent {
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  loginError = signal<string | null>(null);

  loginForm = new FormGroup({
    username: new FormControl('', [Validators.required]),
    password: new FormControl('', [Validators.required, Validators.minLength(8)]),
  });

  async handleSubmit() {
    this.http
      .post(`${env.apiUrl}/auth/login`, this.loginForm.value, {
        responseType: 'text',
      })
      .subscribe({
        next: async (res) => {
          this.authService.login(res);
        },
        error: (err) => {
          this.loginError.set(this.getLoginErrorMessage(err));
        },
      });
  }

  closeErrorModal() {
    this.loginError.set(null);
  }

  private getLoginErrorMessage(error: unknown) {
    if (!(error instanceof HttpErrorResponse)) {
      return 'Unable to log in';
    }

    if (typeof error.error === 'string') {
      try {
        const parsed = JSON.parse(error.error) as { detail?: string };
        return parsed.detail ?? 'Unable to log in';
      } catch {
        return error.error || 'Unable to log in';
      }
    }

    return error.error?.detail ?? 'Unable to log in';
  }
}
