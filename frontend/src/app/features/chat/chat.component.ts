import { formatDate } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ChatService } from '@core/services/chat.service';
import { UserService } from '@core/services/user.service';
import { SidebarComponent } from '@shared/components/sidebar/sidebar.component';
import { Chat, Message, MessageReceipt } from '@shared/types/api.types';
import { forkJoin, switchMap } from 'rxjs';

@Component({
  templateUrl: './chat.component.html',
  imports: [SidebarComponent],
})
export class ChatComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly chatService = inject(ChatService);
  private readonly userService = inject(UserService);

  currentUser = this.userService.getLoggedInUser();

  chat = signal<Chat | null>(null);
  chatTitle = signal<string>('');
  chatImage = signal<string>('');
  selectedChatId = signal<string | null>(null);
  messages = signal<Message[]>([]);
  messageReceipts = signal<MessageReceipt[]>([]);

  ngOnInit() {
    this.route.paramMap
      .pipe(
        switchMap((params) => {
          const chatId = params.get('id');

          if (!chatId) {
            throw new Error('Chat ID missing');
          }

          return forkJoin({
            chat: this.chatService.getById(chatId),
            messages: this.chatService.getMessagesByChatId(chatId),
            messageReceipts: this.chatService.getMessageReceiptsByChatId(chatId),
          });
        }),
      )
      .subscribe({
        next: ({ chat, messages, messageReceipts }) => {
          this.chat.set(chat);
          this.selectedChatId.set(chat.id);
          this.messages.set(messages);
          this.messageReceipts.set(messageReceipts);

          this.setupInfo(chat);

          console.log(chat);
        },
        error: (err) => {
          console.error(err);
          if (err.status === 404) {
            this.router.navigate(['/']);
          }
        },
      });
  }

  private setupInfo(chat: Chat) {
    if (!chat) {
      throw new Error('error loading chat');
    }

    if (chat.type === 'DIRECT') {
      const recipient = chat.members.filter((m) => m.userId !== this.currentUser.id)[0];
      this.chatTitle.set(recipient.fullName);
      this.chatImage.set(recipient.pfpUrl);
    }
  }

  isOwnMessage(message: Message): boolean {
    return message.sender.userId === this.currentUser.id;
  }

  formatMessageTime(value: Date | string): string {
    const date = new Date(value);
    const now = new Date();

    if (this.isSameDay(date, now)) {
      return formatDate(date, 'HH:mm', 'en-US');
    }

    const yesterday = new Date(now);
    yesterday.setDate(now.getDate() - 1);

    if (this.isSameDay(date, yesterday)) {
      return `Yesterday ${formatDate(date, 'HH:mm', 'en-US')}`;
    }

    if (date.getFullYear() === now.getFullYear()) {
      return formatDate(date, 'dd.MM.', 'en-US');
    }

    return formatDate(date, 'dd.MM.yyyy.', 'en-US');
  }

  private isSameDay(a: Date, b: Date): boolean {
    return (
      a.getFullYear() === b.getFullYear() &&
      a.getMonth() === b.getMonth() &&
      a.getDate() === b.getDate()
    );
  }
}
