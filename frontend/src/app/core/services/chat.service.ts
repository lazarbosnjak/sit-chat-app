import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { UserService } from '@core/services/user.service';
import { environment as env } from '@environments/environment';
import {
  Chat,
  Message,
  MessageReceipt,
  MessageStarUpdate,
  StarredMessage,
} from '@shared/types/api.types';
import { firstValueFrom, Observable } from 'rxjs';

interface GroupChatRequest {
  name: string;
  description?: string;
  imageUrl?: string;
  memberIds: string[];
}

interface GroupChatUpdateRequest {
  name: string;
  description?: string;
  imageUrl?: string;
}

@Injectable({
  providedIn: 'root',
})
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly userService = inject(UserService);

  chats = signal<Chat[]>([]);

  async createDirectChat(recipientId: string): Promise<Chat> {
    return firstValueFrom(
      this.http.post<Chat>(`${env.apiUrl}/chats`, {
        name: '',
        description: '',
        imageUrl: '',
        memberIds: [recipientId],
        type: 'DIRECT',
      }),
    );
  }

  async createGroupChat(request: GroupChatRequest): Promise<Chat> {
    return firstValueFrom(
      this.http.post<Chat>(`${env.apiUrl}/chats`, {
        name: request.name,
        description: request.description ?? '',
        imageUrl: request.imageUrl ?? '',
        memberIds: request.memberIds,
        type: 'GROUP',
      }),
    );
  }

  updateGroupChat(chatId: string, request: GroupChatUpdateRequest): Observable<Chat> {
    return this.http.patch<Chat>(`${env.apiUrl}/chats/${chatId}/group`, {
      name: request.name,
      description: request.description ?? '',
      imageUrl: request.imageUrl ?? '',
    });
  }

  addGroupMembers(chatId: string, memberIds: string[]): Observable<Chat> {
    return this.http.post<Chat>(`${env.apiUrl}/chats/${chatId}/members`, {
      memberIds,
    });
  }

  removeGroupMember(chatId: string, memberId: string): Observable<Chat> {
    return this.http.delete<Chat>(`${env.apiUrl}/chats/${chatId}/members/${memberId}`);
  }

  updateGroupMemberRole(
    chatId: string,
    memberId: string,
    role: 'ADMIN' | 'MEMBER',
  ): Observable<Chat> {
    return this.http.patch<Chat>(`${env.apiUrl}/chats/${chatId}/members/${memberId}/role`, {
      role,
    });
  }

  getMyChats() {
    return this.http.get<Chat[]>(`${env.apiUrl}/chats/me`);
  }

  loadMyChats(): void {
    this.getMyChats().subscribe({
      next: (chats) => this.chats.set(chats),
      error: (err) => console.error('Could not load chats', err),
    });
  }

  markChatAsReadLocally(chatId: string): void {
    this.chats.update((chats) =>
      chats.map((chat) => (chat.id === chatId ? { ...chat, unreadCount: 0 } : chat)),
    );
  }

  updateChatUnreadCount(chatId: string, unreadCount: number): void {
    this.chats.update((chats) =>
      chats.map((chat) => (chat.id === chatId ? { ...chat, unreadCount } : chat)),
    );
  }

  getById(chatId: string): Observable<Chat> {
    return this.http.get<Chat>(`${env.apiUrl}/chats/${chatId}`);
  }

  // getWithMessagesById(chatId: string): Observable<ChatWithMessages> {
  //   return this.http.get<ChatWithMessages>(`${env.apiUrl}/chats/${chatId}`);
  // }

  getMessagesByChatId(chatId: string): Observable<Message[]> {
    return this.http.get<Message[]>(`${env.apiUrl}/chats/${chatId}/messages`);
  }

  getMessageReceiptsByChatId(chatId: string): Observable<MessageReceipt[]> {
    return this.http.get<MessageReceipt[]>(`${env.apiUrl}/chats/${chatId}/message-receipts`);
  }

  getStarredMessages(): Observable<StarredMessage[]> {
    return this.http.get<StarredMessage[]>(`${env.apiUrl}/chats/me/starred-messages`);
  }

  toggleStarredMessage(chatId: string, messageId: string): Observable<MessageStarUpdate> {
    return this.http.patch<MessageStarUpdate>(
      `${env.apiUrl}/chats/${chatId}/messages/${messageId}/star`,
      {},
    );
  }

  getChatPfpUrl(chat: Chat): string {
    const currentUser = this.userService.getLoggedInUser();

    if (chat.type === 'DIRECT') {
      return chat.members.filter((m) => m.username !== currentUser.username)[0]?.pfpUrl ?? '';
    }
    if (chat.type === 'GROUP') {
      return chat.imageUrl ? chat.imageUrl : '';
    }
    return '';
  }

  getChatTitle(chat: Chat): string {
    const currentUser = this.userService.getLoggedInUser();

    if (chat.type === 'DIRECT') {
      return chat.members.filter((m) => m.username !== currentUser.username)[0]?.fullName ?? 'Chat';
    }
    if (chat.type === 'GROUP') {
      return chat.name ? chat.name : 'Group Chat';
    }
    return '';
  }

  markAsRead(chatId: string) {
    return this.http.patch<void>(`${env.apiUrl}/chats/${chatId}/read`, {});
  }
}
