import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment as env } from '@environments/environment';
import { Chat } from '@shared/types/api.types';

@Injectable({
  providedIn: 'root',
})
export class ChatService {
  private readonly http = inject(HttpClient);

  async createDirectChat(recipientId: string): Promise<Chat> {
    return firstValueFrom(
      this.http.post<Chat>(`${env.apiUrl}/chats`, {
        name: '',
        imageUrl: '',
        memberIds: [recipientId],
        type: 'DIRECT',
      }),
    );
  }

  async getMyChats(): Promise<Chat[]> {
    return firstValueFrom(this.http.get<Chat[]>(`${env.apiUrl}/chats/me`));
  }
}
