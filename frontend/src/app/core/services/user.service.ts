import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { User } from '@shared/types/api.types';
import { environment as env } from '@environments/environment';
import { firstValueFrom } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ChatService {
  private readonly http = inject(HttpClient);

  async getLoggedInUser(): Promise<User> {
    return firstValueFrom(this.http.get<User>(`${env.apiUrl}/users/me`));
  }
}
