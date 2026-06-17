import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom, Observable } from 'rxjs';
import { environment as env } from '@environments/environment';
import { Chat, User } from '@shared/types/api.types';
import { UserService } from '@core/services/user.service';

@Injectable({
  providedIn: 'root',
})
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly userService = inject(UserService);

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

  getById(chatId: string): Observable<Chat> {
    return this.http.get<Chat>(`${env.apiUrl}/chats/${chatId}`);
  }

  getChatPfpUrl(chat: Chat): string {
    const currentUser = this.userService.getLoggedInUser();

    if (chat.type === 'DIRECT') {
      return chat.members.filter((m) => m.username !== currentUser.username)[0].pfpUrl;
    }
    if (chat.type === 'GROUP') {
      return chat.imageUrl;
    }
    return '';
  }

  getChatTitle(chat: Chat): string {
    const currentUser = this.userService.getLoggedInUser();

    if (chat.type === 'DIRECT') {
      console.log('User', currentUser);

      console.log(chat.members.filter((m) => m.username !== currentUser.username));

      return chat.members.filter((m) => m.username !== currentUser.username)[0].fullName;
    }
    if (chat.type === 'GROUP') {
      return chat.name;
    }
    return '';
  }
}
