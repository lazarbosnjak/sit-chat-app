import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ChatService } from '@core/services/chat.service';
import { UserService } from '@core/services/user.service';
import { SidebarComponent } from '@shared/components/sidebar/sidebar.component';
import { Chat } from '@shared/types/api.types';
import { switchMap } from 'rxjs';

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

  ngOnInit() {
    this.route.paramMap
      .pipe(
        switchMap((params) => {
          const chatId = params.get('id');

          if (!chatId) {
            throw new Error('Chat ID missing');
          }

          return this.chatService.getById(chatId);
        }),
      )
      .subscribe({
        next: (chat) => {
          this.chat.set(chat);
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
}
