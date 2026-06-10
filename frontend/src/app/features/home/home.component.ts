import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { environment as env } from '@environments/environment';

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
    this.http.get<User>(`${env.apiUrl}/users/me`).subscribe({
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
