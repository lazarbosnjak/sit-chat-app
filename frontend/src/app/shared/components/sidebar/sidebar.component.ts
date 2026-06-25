import { HttpClient } from '@angular/common/http';
import { Component, effect, inject, input, signal } from '@angular/core';
import { form, FormField } from '@angular/forms/signals';
import { Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { ChatService } from '@core/services/chat.service';
import { UserService } from '@core/services/user.service';
import { environment as env } from '@environments/environment';
import { ModalComponent } from '@shared/components/modal/modal.component';
import { Chat, User } from '@shared/types/api.types';

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  imports: [ModalComponent, FormField],
})
export class SidebarComponent {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  readonly userService = inject(UserService);
  readonly chatService = inject(ChatService);
  readonly authService = inject(AuthService);

  selectedChatId = input<string | null>(null);

  currentUser = signal<User | null>(null);

  chats = this.chatService.chats;

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
      this.searchModel.set({ content: '' });
      this.searchResults.set([]);
      this.closeNewChatModal();
    } catch (err) {
      console.error(err);
    }
  }

  async ngOnInit() {
    this.chatService.loadMyChats();
    this.currentUser.set(this.userService.getLoggedInUser());
  }

  navigateToChat(chatId: string) {
    this.router.navigate(['/chats', chatId]);
  }

  isSelectedChat(chat: Chat): boolean {
    return chat.id === this.selectedChatId();
  }

  navigateToProfile() {
    this.router.navigate(['/profile']);
  }
}
