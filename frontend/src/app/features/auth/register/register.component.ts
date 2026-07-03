import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import {
  debounce,
  disabled,
  email,
  form,
  FormField,
  FormRoot,
  minLength,
  pattern,
  required,
  validateTree,
} from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { environment as env } from '@environments/environment';
import { ModalComponent } from '@shared/components/modal/modal.component';
import { firstValueFrom } from 'rxjs';

interface RegisterModel {
  username: string;
  password: string;
  repeatedPassword: string;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  pfpUrl: string;
}

type RegisterModal = {
  title: string;
  message: string;
  variant: 'default' | 'error';
};

const EMPTY_REGISTER_MODEL: RegisterModel = {
  username: '',
  password: '',
  repeatedPassword: '',
  email: '',
  firstName: '',
  lastName: '',
  phoneNumber: '',
  pfpUrl: '',
};

@Component({
  templateUrl: './register.component.html',
  imports: [RouterLink, ModalComponent, FormField, FormRoot],
})
export class RegisterComponent {
  private http = inject(HttpClient);
  private readonly urlRegexp = new RegExp(
    /^https?:\/\/(www\.)?[-a-zA-Z0-9@:%._\+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_\+.~#?&//=]*)$/,
  );

  registerModel = signal<RegisterModel>({ ...EMPTY_REGISTER_MODEL });
  registerForm = form(
    this.registerModel,
    (schemaPath) => {
      debounce(schemaPath, 'blur');
      disabled(schemaPath, { when: ({ state }) => state.submitting() });

      required(schemaPath.firstName, { message: 'First name is required' });
      required(schemaPath.lastName, { message: 'Last name is required' });
      required(schemaPath.email, { message: 'E-mail is required' });
      required(schemaPath.username, { message: 'Username is required' });
      required(schemaPath.password, { message: 'Password is required' });
      required(schemaPath.repeatedPassword, { message: 'Repeated password is required' });

      email(schemaPath.email, { message: 'Enter a valid e-mail address' });
      minLength(schemaPath.password, 8, {
        message: 'Password must be at least 8 characters',
      });
      minLength(schemaPath.repeatedPassword, 8, {
        message: 'Repeated password must be at least 8 characters',
      });
      pattern(schemaPath.pfpUrl, this.urlRegexp, {
        message: 'Enter a valid profile picture URL',
        when: ({ value }) => value().trim().length > 0,
      });

      validateTree(schemaPath, ({ value, fieldTree }) => {
        const { password, repeatedPassword } = value();

        if (password === repeatedPassword) {
          return;
        }

        return {
          fieldTree: fieldTree.repeatedPassword,
          kind: 'passwordMismatch',
          message: 'Passwords must match',
        };
      });
    },
    {
      submission: {
        action: async (field) => {
          this.registerModal.set(null);

          try {
            await firstValueFrom(this.http.post(`${env.apiUrl}/auth/register`, field().value()));
            this.registerModal.set({
              title: 'Registration successful',
              message: 'Your account has been created. You can now log in.',
              variant: 'default',
            });
          } catch (err) {
            this.registerModal.set({
              title: 'Registration failed',
              message: this.getRegistrationErrorMessage(err),
              variant: 'error',
            });
          }
        },
        onInvalid: (field) => {
          field().markAsTouched();
          field().errorSummary()[0]?.fieldTree().focusBoundControl();
        },
      },
    },
  );
  registerModal = signal<RegisterModal | null>(null);

  closeRegisterModal() {
    this.registerModal.set(null);
  }

  showError(field: { touched: () => boolean; invalid: () => boolean }) {
    return field.invalid() && field.touched();
  }

  firstError(field: { errors: () => readonly { message?: string; kind: string }[] }) {
    return field.errors()[0]?.message ?? 'Invalid value';
  }

  private getRegistrationErrorMessage(error: unknown) {
    if (!(error instanceof HttpErrorResponse)) {
      return 'Unable to register';
    }

    if (typeof error.error === 'string') {
      try {
        const parsed = JSON.parse(error.error) as { detail?: string };
        return parsed.detail ?? 'Unable to register';
      } catch {
        return error.error || 'Unable to register';
      }
    }

    return error.error?.detail ?? 'Unable to register';
  }
}
