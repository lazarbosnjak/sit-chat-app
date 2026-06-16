import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment as env } from '@environments/environment';

interface Chat {
  id: string;
  name: string;
  imageUrl: string;
  type: 'DIRECT' | 'GROUP';
  createdAt: Date;
  members: ChatMember[];
}

interface ChatMember {
  userId: string;
  username: string;
  role: 'ADMIN' | 'MEMBER';
}

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
}
