import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { User } from '@shared/types/api.types';
import { environment as env } from '@environments/environment';
import { firstValueFrom } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly LOCAL_STORAGE_USER_KEY = 'current_user';

  getLoggedInUser(): User {
    const lsItem = localStorage.getItem(this.LOCAL_STORAGE_USER_KEY);
    return JSON.parse(lsItem!) as User;
  }

  async setUserToLocalStorage(user?: User): Promise<void> {
    if (!user) {
      user = await firstValueFrom(this.http.get<User>(`${env.apiUrl}/users/me`));
    }
    localStorage.setItem(this.LOCAL_STORAGE_USER_KEY, JSON.stringify(user));
  }

  removeUserFromLocalStorage(): void {
    localStorage.removeItem(this.LOCAL_STORAGE_USER_KEY);
  }

  async updateUser(user: User): Promise<User> {
    const savedUser = await firstValueFrom(
      this.http.put<User>(`${env.apiUrl}/users/${user.id}`, user),
    );

    await this.setUserToLocalStorage(savedUser);

    return savedUser;
  }

  getUserById(userId: string) {
    return this.http.get<User>(`${env.apiUrl}/users/${userId}`);
  }

  searchUsers(search: string) {
    return this.http.get<User[]>(`${env.apiUrl}/users`, {
      params: {
        search,
      },
    });
  }

  getAllUsers() {
    const user = this.getLoggedInUser();
    if (user.role !== 'ADMIN') {
      throw new Error('not admin');
    }
    return this.http.get<User[]>(`${env.apiUrl}/admin/users`);
  }
}
