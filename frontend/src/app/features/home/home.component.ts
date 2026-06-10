import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';

interface User {
  username: string;
}

@Component({
  templateUrl: './home.component.html',
})
export class HomeComponent {
  private http = inject(HttpClient);
  username = signal('');

  ngOnInit() {
    this.http.get<User>('http://localhost:8080/api/v0/users/me').subscribe({
      next: (user) => {
        console.log(user.username);
        this.username.set(user.username);
      },
      error: (err: Error) => {
        console.error(err);
      },
    });
  }
}
