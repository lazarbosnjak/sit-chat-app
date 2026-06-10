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
  private readonly TOKEN_KEY = 'token';

  isAuthenticated(): boolean {
    const token = this.getToken();

    if (!token) {
      return false;
    }

    try {
      const payload = jwtDecode<JwtPayload>(token);
      const expired = payload.exp * 1000 < Date.now();
      if (!expired) {
        return true;
      } else {
        this.removeToken();
        return false;
      }
    } catch (error) {
      this.removeToken();
      console.error(error);
      return false;
    }
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  removeToken(): void {
    localStorage.removeItem(this.TOKEN_KEY);
  }

  login(res: string) {
    localStorage.setItem(this.TOKEN_KEY, res);
    this.router.navigate(['/']);
  }

  logout() {
    this.removeToken();
    this.router.navigate(['/auth/login']);
  }
}
