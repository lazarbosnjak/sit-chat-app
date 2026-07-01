import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { environment as env } from '@environments/environment';

interface ApiMessageResponse {
  message: string;
}

@Component({
  templateUrl: './reset-password.component.html',
  imports: [ReactiveFormsModule, RouterLink],
})
export class ResetPasswordComponent {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);

  token = signal<string | null>(this.route.snapshot.queryParamMap.get('token'));
  statusMessage = signal<string | null>(null);
  statusType = signal<'success' | 'error' | null>(null);
  isSubmitting = signal(false);

  requestForm = new FormGroup({
    identifier: new FormControl('', [Validators.required]),
  });

  resetForm = new FormGroup(
    {
      password: new FormControl('', [Validators.required, Validators.minLength(8)]),
      repeatedPassword: new FormControl('', [Validators.required, Validators.minLength(8)]),
    },
    {
      validators: passwordsMatchValidator(),
    },
  );

  requestResetLink() {
    this.statusMessage.set(null);

    if (this.requestForm.invalid) {
      this.requestForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.http
      .post<ApiMessageResponse>(`${env.apiUrl}/auth/password-reset/request`, {
        identifier: this.requestForm.controls.identifier.value,
      })
      .subscribe({
        next: (res) => {
          this.statusType.set('success');
          this.statusMessage.set(res.message);
          this.requestForm.reset();
          this.isSubmitting.set(false);
        },
        error: (err) => {
          this.statusType.set('error');
          this.statusMessage.set(this.getErrorMessage(err));
          this.isSubmitting.set(false);
        },
      });
  }

  resetPassword() {
    this.statusMessage.set(null);

    const token = this.token();
    if (!token) {
      this.statusType.set('error');
      this.statusMessage.set('Password reset link is missing.');
      return;
    }

    if (this.resetForm.invalid) {
      this.resetForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.http
      .post<ApiMessageResponse>(`${env.apiUrl}/auth/password-reset/confirm`, {
        token,
        password: this.resetForm.controls.password.value,
        repeatedPassword: this.resetForm.controls.repeatedPassword.value,
      })
      .subscribe({
        next: (res) => {
          this.statusType.set('success');
          this.statusMessage.set(res.message);
          this.resetForm.reset();
          this.isSubmitting.set(false);
        },
        error: (err) => {
          this.statusType.set('error');
          this.statusMessage.set(this.getErrorMessage(err));
          this.isSubmitting.set(false);
        },
      });
  }

  isRequestInvalid(name: string): boolean {
    const control = this.requestForm.get(name);
    return !!control && control.touched && control.invalid;
  }

  isResetInvalid(name: string): boolean {
    const control = this.resetForm.get(name);
    return !!control && control.touched && control.invalid;
  }

  passwordsMismatch(): boolean {
    return this.resetForm.touched && this.resetForm.hasError('passwordMistmatch');
  }

  private getErrorMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return 'Unable to reset password';
    }

    if (typeof error.error === 'string') {
      try {
        const parsed = JSON.parse(error.error) as { detail?: string };
        return parsed.detail ?? 'Unable to reset password';
      } catch {
        return error.error || 'Unable to reset password';
      }
    }

    return error.error?.detail ?? 'Unable to reset password';
  }
}

function passwordsMatchValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const password = control.get('password')?.value;
    const repeatedPassword = control.get('repeatedPassword')?.value;

    return password === repeatedPassword ? null : { passwordMistmatch: true };
  };
}
