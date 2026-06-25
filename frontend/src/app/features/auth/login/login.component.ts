import { HttpClient } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { environment as env } from '@environments/environment';

@Component({
  templateUrl: './login.component.html',
  imports: [ReactiveFormsModule, RouterLink],
})
export class LoginComponent {
  private http = inject(HttpClient);
  private authService = inject(AuthService);

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
          alert('fail');
          console.log(err);
        },
      });
  }
}
