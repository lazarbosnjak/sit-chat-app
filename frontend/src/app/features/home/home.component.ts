import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { form, FormField, FormRoot } from '@angular/forms/signals';
import { AuthService } from '@core/services/auth.service';
import { ChatService } from '@core/services/chat.service';
import { StompService } from '@core/services/stomp.service';
import { UserService } from '@core/services/user.service';
import { environment as env } from '@environments/environment';
import { ModalComponent } from '@shared/components/modal/modal.component';
import { Chat } from '@shared/types/api.types';
import { StompSubscription } from '@stomp/stompjs';

interface User {
  id: string;
  username: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  email: string;
  pfpUrl: string;
  role: 'ADMIN' | 'USER';
  createdAt: Date;
  enabled: boolean;
}

@Component({
  templateUrl: './home.component.html',
  imports: [FormField, ModalComponent],
})
export class HomeComponent {
  private readonly http = inject(HttpClient);
  private readonly chatService = inject(ChatService);
  readonly userService = inject(UserService);
  readonly authService = inject(AuthService);

  currentUser = signal<User | null>(null);

  chats = signal<Chat[]>([]);

  // private readonly destroyRef = inject(DestroyRef);
  // chatModel = signal({
  //   content: '',
  // });
  // chatForm = form(this.chatModel, {
  //   submission: {
  //     action: async (field) => {
  //       this.sendMessage(field().value().content);
  //     },
  //   },
  // });
  //
  // chatMessage = signal('');
  //
  // private readonly stompService = inject(StompService);
  // private chatSubscription: StompSubscription | null = null;

  async ngOnInit() {
    this.currentUser.set(this.userService.getLoggedInUser());

    try {
      const chats = await this.chatService.getMyChats();
      this.chats.set(chats);
    } catch (err) {
      console.error(err);
      // TODO: Modal for error handling
    }

    // this.stompService.connect(() => {
    //   this.chatSubscription = this.stompService.subscribe('/topic/messages', (message) => {
    //     this.chatMessage.set(message.body);
    //   });
    //
    //   this.destroyRef.onDestroy(() => {
    //     this.chatSubscription?.unsubscribe();
    //   });
    // });
  }

  //
  // sendMessage(content: string) {
  //   this.stompService.publish('/app/chat', content);
  // }

  // SEARCH
  searchModel = signal({
    content: '',
  });
  searchForm = form(this.searchModel);
  searchResults = signal<User[]>([]);
  isSearching = signal(false);

  constructor() {
    effect((onCleanup) => {
      const search = this.searchModel().content;
      const DEBOUNCE_TIMER = 300;
      this.isSearching.set(true);

      if (!search) {
        this.searchResults.set([]);
        this.isSearching.set(false);
        return;
      }

      const timeoutId = setTimeout(() => {
        this.isSearching.set(true);

        const subscribtion = this.http
          .get<User[]>(`${env.apiUrl}/users`, {
            params: {
              search: search,
            },
          })
          .subscribe({
            next: (users) => {
              this.searchResults.set(users);
              this.isSearching.set(false);
            },
            error: (err) => {
              this.searchResults.set([]);
              this.isSearching.set(false);
              console.error(err);
            },
          });
        onCleanup(() => subscribtion.unsubscribe());
      }, DEBOUNCE_TIMER);

      onCleanup(() => clearTimeout(timeoutId));
    });
  }

  isNewChatModalOpen = signal(false);

  openNewChatModal() {
    this.isNewChatModalOpen.set(true);
  }

  closeNewChatModal() {
    this.isNewChatModalOpen.set(false);
  }

  async createNewDirectChat(userId: string) {
    try {
      const chat = await this.chatService.createDirectChat(userId);
      this.chats.update((old) => [...old, chat]);
      this.closeNewChatModal();
    } catch (err) {
      console.error(err);
    }
  }
}
