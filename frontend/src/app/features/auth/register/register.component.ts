import { HttpClient } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { environment as env } from '@environments/environment';

@Component({
  templateUrl: './register.component.html',
  imports: [ReactiveFormsModule, RouterLink],
})
export class RegisterComponent {
  private http = inject(HttpClient);
  private readonly urlRegexp = new RegExp(
    /^https?:\/\/(www\.)?[-a-zA-Z0-9@:%._\+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_\+.~#?&//=]*)$/,
  );
  registerForm = new FormGroup(
    {
      username: new FormControl<string>('', [Validators.required]),
      password: new FormControl<string>('', [Validators.required, Validators.minLength(8)]),
      repeatedPassword: new FormControl<string>('', [Validators.required, Validators.minLength(8)]),
      email: new FormControl<string>('', [Validators.required, Validators.email]),
      firstName: new FormControl<string>('', [Validators.required]),
      lastName: new FormControl<string>('', [Validators.required]),
      phoneNumber: new FormControl<string>(''),
      pfpUrl: new FormControl<string>('', [Validators.pattern(this.urlRegexp)]),
    },
    {
      validators: passwordsMatchValidator(),
    },
  );

  handleSubmit() {
    this.http.post(`${env.apiUrl}/auth/register`, this.registerForm.value).subscribe({
      next: () => {
        alert('Success');
      },
      error: () => {
        alert('Fail');
      },
    });
  }

  isInvalid(name: string): boolean {
    const control = this.registerForm.get(name);
    return !!control && control.touched && control.invalid;
  }
}

function passwordsMatchValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const password = control.get('password')?.value;
    const repeatedPassword = control.get('repeatedPassword')?.value;

    return password === repeatedPassword ? null : { passwordMistmatch: true };
  };
}
