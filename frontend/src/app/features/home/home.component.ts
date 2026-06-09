import { Component } from '@angular/core';
import { jwtDecode } from 'jwt-decode';

interface JwtPayload {
  sub: string;
  role: string;
  exp: number;
}

@Component({
  templateUrl: './home.component.html',
})
export class HomeComponent {
  username = '';

  ngOnInit() {
    const token = localStorage.getItem('token');

    if (token) {
      const payload = jwtDecode<JwtPayload>(token);
      this.username = payload.sub;
    }
  }
}
