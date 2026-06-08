import { HttpClient } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

@Component({
  templateUrl: './login.component.html',
  imports: [ReactiveFormsModule, RouterLink],
})
export class LoginComponent {
  private http = inject(HttpClient);

  loginForm = new FormGroup({
    username: new FormControl(''),
    password: new FormControl(''),
  });

  async handleSubmit() {
    this.http
      .post('http://localhost:8080/api/v0/auth/login', this.loginForm.value, {
        responseType: 'text',
      })
      .subscribe({
        next: () => alert('Success'),
        error: (err) => {
          alert('fail');
          console.log(err);
        },
      });
  }
}
