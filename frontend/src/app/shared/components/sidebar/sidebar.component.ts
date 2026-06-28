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
import { LucideGavel, LucideHouse, LucideMessageCirclePlus } from '@lucide/angular';

type ProfilePictureFilter = 'ANY' | 'SET' | 'NOT_SET';

interface UserSearchModel {
  content: string;
  lastActiveFrom: string;
  lastActiveTo: string;
  profilePicture: ProfilePictureFilter;
}

const EMPTY_SEARCH_MODEL: UserSearchModel = {
  content: '',
  lastActiveFrom: '',
  lastActiveTo: '',
  profilePicture: 'ANY',
};

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  imports: [ModalComponent, FormField, LucideHouse, LucideMessageCirclePlus, LucideGavel],
})
export class SidebarComponent {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  readonly userService = inject(UserService);
  readonly chatService = inject(ChatService);
  readonly authService = inject(AuthService);
  readonly ROLE_ADMIN = 'ROLE_ADMIN';

  selectedChatId = input<string | null>(null);

  currentUser = signal<User | null>(null);

  chats = this.chatService.chats;

  // SEARCH
  searchModel = signal<UserSearchModel>({ ...EMPTY_SEARCH_MODEL });
  searchForm = form(this.searchModel);
  searchResults = signal<User[]>([]);
  isSearching = signal(false);

  constructor() {
    effect((onCleanup) => {
      const params = this.buildSearchParams(this.searchModel());
      const DEBOUNCE_TIMER = 300;

      if (Object.keys(params).length === 0) {
        this.searchResults.set([]);
        this.isSearching.set(false);
        return;
      }

      this.isSearching.set(true);
      let subscription: { unsubscribe: () => void } | null = null;

      const timeoutId = setTimeout(() => {
        this.isSearching.set(true);

        subscription = this.http
          .get<User[]>(`${env.apiUrl}/users`, {
            params,
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
      }, DEBOUNCE_TIMER);

      onCleanup(() => {
        clearTimeout(timeoutId);
        subscription?.unsubscribe();
      });
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
      this.searchModel.set({ ...EMPTY_SEARCH_MODEL });
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

  isSelectedChat(chat: Chat): boolean {
    return chat.id === this.selectedChatId();
  }

  navigateTo(location: string) {
    this.router.navigate([location]);
  }

  hasSearchCriteria(): boolean {
    return Object.keys(this.buildSearchParams(this.searchModel())).length > 0;
  }

  formatLastActive(lastActiveAt: Date | string | null | undefined): string {
    if (!lastActiveAt) {
      return 'Never active';
    }

    const date = new Date(lastActiveAt);

    if (Number.isNaN(date.getTime())) {
      return 'Last activity unavailable';
    }

    return `Last active ${date.toLocaleDateString()}`;
  }

  private buildSearchParams(search: UserSearchModel): Record<string, string> {
    const params: Record<string, string> = {};
    const content = search.content.trim();

    if (content) {
      params['search'] = content;
    }
    if (search.lastActiveFrom) {
      params['lastActiveFrom'] = search.lastActiveFrom;
    }
    if (search.lastActiveTo) {
      params['lastActiveTo'] = search.lastActiveTo;
    }
    if (search.profilePicture === 'SET') {
      params['hasProfilePicture'] = 'true';
    }
    if (search.profilePicture === 'NOT_SET') {
      params['hasProfilePicture'] = 'false';
    }

    return params;
  }
}
