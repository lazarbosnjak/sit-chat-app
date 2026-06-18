import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { TOKEN_KEY } from '@core/constants/auth.constants';
import { StompService } from '@core/services/stomp.service';
import { UserService } from '@core/services/user.service';
import { jwtDecode } from 'jwt-decode';

interface JwtPayload {
  sub: string;
  role: {
    authority: string;
  };
  exp: number;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private router = inject(Router);
  private readonly userService = inject(UserService);
  private readonly stompService = inject(StompService);

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
        this.logout();
        return false;
      }
    } catch (error) {
      this.logout();
      console.error(error);
      return false;
    }
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  removeToken(): void {
    localStorage.removeItem(TOKEN_KEY);
  }

  async login(res: string) {
    localStorage.setItem(TOKEN_KEY, res);
    await this.userService.setUserToLocalStorage();
    this.router.navigate(['/']);
  }

  async logout(): Promise<void> {
    await this.stompService.disconnect();
    this.removeToken();
    this.userService.removeUserFromLocalStorage();
    this.router.navigate(['/auth/login']);
  }

  hasRole(role: string): boolean {
    const token = this.getToken();
    if (!token) {
      return false;
    }
    const payload = jwtDecode<JwtPayload>(token);
    return payload.role.authority === role.toLocaleUpperCase();
  }
}
