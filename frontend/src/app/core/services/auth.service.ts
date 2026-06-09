import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { jwtDecode } from 'jwt-decode';

interface JwtPayload {
  sub: string;
  role: string;
  exp: number;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private router = inject(Router);

  isAuthenticated(): boolean {
    const token = this.getToken();

    if (!token) {
      return false;
    }

    try {
      const payload = jwtDecode<JwtPayload>(token);
      return payload.exp * 1000 > Date.now();
    } catch (error) {
      console.error(error);
      return false;
    }
  }

  getToken(): string | null {
    const tokenKey = 'token';
    return localStorage.getItem(tokenKey);
  }

  login(res: string) {
    localStorage.setItem('token', res);
    this.router.navigate(['/']);
  }

  logout() {
    localStorage.removeItem('token');
    this.router.navigate(['/auth/login']);
  }
}
