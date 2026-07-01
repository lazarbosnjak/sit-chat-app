import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { ChatService } from '@core/services/chat.service';
import { Chat } from '@shared/types/api.types';

type InviteJoinStatus = 'joining' | 'joined' | 'error';

@Component({
  templateUrl: './group-invite.component.html',
})
export class GroupInviteComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly chatService = inject(ChatService);
  private readonly destroyRef = inject(DestroyRef);

  status = signal<InviteJoinStatus>('joining');
  error = signal<string | null>(null);
  joinedChat = signal<Chat | null>(null);

  ngOnInit(): void {
    const token = this.route.snapshot.paramMap.get('token');

    if (!token) {
      this.status.set('error');
      this.error.set('Invite link is invalid or revoked.');
      return;
    }

    this.chatService
      .joinGroupByInviteToken(token)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (chat) => {
          this.joinedChat.set(chat);
          this.status.set('joined');
          this.upsertChat(chat);
          this.router.navigate(['/chats', chat.id]);
        },
        error: (err) => {
          console.error('Could not join group by invite link', err);
          this.status.set('error');
          this.error.set(this.getErrorMessage(err, 'Invite link is invalid or revoked.'));
        },
      });
  }

  goHome(): void {
    this.router.navigate(['/']);
  }

  openJoinedChat(): void {
    const chat = this.joinedChat();

    if (!chat) {
      return;
    }

    this.router.navigate(['/chats', chat.id]);
  }

  private upsertChat(joinedChat: Chat): void {
    this.chatService.chats.update((chats) => {
      const chatExists = chats.some((chat) => chat.id === joinedChat.id);

      if (chatExists) {
        return chats.map((chat) => (chat.id === joinedChat.id ? joinedChat : chat));
      }

      return [joinedChat, ...chats];
    });
  }

  private getErrorMessage(err: unknown, fallback: string): string {
    if (typeof err === 'object' && err !== null && 'error' in err) {
      const error = (err as { error?: { detail?: string } }).error;

      if (error?.detail) {
        return error.detail;
      }
    }

    return fallback;
  }
}
