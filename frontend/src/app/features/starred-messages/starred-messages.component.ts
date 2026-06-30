import { formatDate } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { ChatService } from '@core/services/chat.service';
import { SidebarComponent } from '@shared/components/sidebar/sidebar.component';
import { Message, StarredMessage } from '@shared/types/api.types';
import { LucideMessageSquare, LucideStar } from '@lucide/angular';

@Component({
  templateUrl: './starred-messages.component.html',
  imports: [SidebarComponent, LucideMessageSquare, LucideStar],
})
export class StarredMessagesComponent {
  readonly chatService = inject(ChatService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  starredMessages = signal<StarredMessage[]>([]);
  isLoading = signal(false);
  errorMessage = signal('');

  ngOnInit(): void {
    this.chatService.loadMyChats();
    this.loadStarredMessages();
  }

  openChat(chatId: string): void {
    this.router.navigate(['/chats', chatId]);
  }

  formatMessageDate(value: Date | string): string {
    return formatDate(value, 'dd.MM.yyyy. HH:mm', 'en-US');
  }

  private loadStarredMessages(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.chatService
      .getStarredMessages()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (messages) => {
          this.starredMessages.set(
            messages.map((starredMessage) => ({
              ...starredMessage,
              message: this.normalizeMessage(starredMessage.message),
            })),
          );
          this.isLoading.set(false);
        },
        error: (err) => {
          this.errorMessage.set('Could not load starred messages.');
          this.isLoading.set(false);
          console.error(err);
        },
      });
  }

  private normalizeMessage(message: Message): Message {
    return {
      ...message,
      reactions: message.reactions ?? [],
      starredByMe: message.starredByMe ?? true,
      systemMessage: message.systemMessage ?? false,
    };
  }
}
