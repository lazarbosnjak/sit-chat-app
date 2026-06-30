import { HttpClient } from '@angular/common/http';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { form, FormField } from '@angular/forms/signals';
import { Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { ChatService } from '@core/services/chat.service';
import { UserService } from '@core/services/user.service';
import { environment as env } from '@environments/environment';
import { ModalComponent } from '@shared/components/modal/modal.component';
import { Chat, ChatMember, User } from '@shared/types/api.types';
import {
  LucideGavel,
  LucideHouse,
  LucideMessageCirclePlus,
  LucideStar,
  LucideUsers,
} from '@lucide/angular';

type ProfilePictureFilter = 'ANY' | 'SET' | 'NOT_SET';

interface UserSearchModel {
  content: string;
  lastActiveFrom: string;
  lastActiveTo: string;
  profilePicture: ProfilePictureFilter;
}

interface GroupChatModel {
  name: string;
  description: string;
  imageUrl: string;
}

interface UserCandidate {
  id: string;
  username: string;
  firstName: string;
  lastName: string;
  fullName: string;
  phoneNumber?: string | null;
  pfpUrl: string;
  lastActiveAt?: Date | string | null;
}

const EMPTY_SEARCH_MODEL: UserSearchModel = {
  content: '',
  lastActiveFrom: '',
  lastActiveTo: '',
  profilePicture: 'ANY',
};

const EMPTY_GROUP_MODEL: GroupChatModel = {
  name: '',
  description: '',
  imageUrl: '',
};

const DEFAULT_PROFILE_PICTURE_URL =
  'https://static.vecteezy.com/system/resources/thumbnails/003/337/584/small/default-avatar-photo-placeholder-profile-icon-vector.jpg';

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  imports: [
    ModalComponent,
    FormField,
    LucideHouse,
    LucideMessageCirclePlus,
    LucideGavel,
    LucideStar,
    LucideUsers,
  ],
})
export class SidebarComponent {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  readonly userService = inject(UserService);
  readonly chatService = inject(ChatService);
  readonly authService = inject(AuthService);
  readonly ROLE_ADMIN = 'ROLE_ADMIN';

  selectedChatId = input<string | null>(null);

  currentUser = signal<User | null>(this.userService.getLoggedInUser());

  chats = this.chatService.chats;

  searchModel = signal<UserSearchModel>({ ...EMPTY_SEARCH_MODEL });
  searchForm = form(this.searchModel);
  searchResults = signal<User[]>([]);
  isSearching = signal(false);

  groupModel = signal<GroupChatModel>({ ...EMPTY_GROUP_MODEL });
  groupForm = form(this.groupModel);
  selectedGroupMembers = signal<UserCandidate[]>([]);
  isCreatingGroup = signal(false);
  groupCreateError = signal<string | null>(null);

  contactUsers = computed(() => {
    const currentUserId = this.currentUser()?.id;
    const usersById = new Map<string, UserCandidate>();

    for (const chat of this.chats()) {
      for (const member of chat.members) {
        if (member.userId === currentUserId || usersById.has(member.userId)) {
          continue;
        }

        usersById.set(member.userId, this.memberToCandidate(member));
      }
    }

    return Array.from(usersById.values()).sort((a, b) =>
      this.getCandidateFullName(a).localeCompare(this.getCandidateFullName(b)),
    );
  });

  filteredContactUsers = computed(() =>
    this.contactUsers().filter((user) => this.matchesSearchCriteria(user, this.searchModel())),
  );

  newUserSearchResults = computed(() => {
    const contactIds = new Set(this.contactUsers().map((user) => user.id));

    return this.searchResults()
      .filter((user) => !contactIds.has(user.id))
      .map((user) => this.userToCandidate(user));
  });

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
  isNewGroupModalOpen = signal(false);

  openNewChatModal() {
    this.closeNewGroupModal();
    this.resetUserSearch();
    this.isNewChatModalOpen.set(true);
  }

  closeNewChatModal() {
    this.isNewChatModalOpen.set(false);
    this.resetUserSearch();
  }

  openNewGroupModal() {
    this.closeNewChatModal();
    this.resetGroupForm();
    this.resetUserSearch();
    this.isNewGroupModalOpen.set(true);
  }

  closeNewGroupModal() {
    this.isNewGroupModalOpen.set(false);
    this.resetGroupForm();
    this.resetUserSearch();
  }

  async createNewDirectChat(userId: string) {
    try {
      const chat = await this.chatService.createDirectChat(userId);
      this.chats.update((old) => [...old, chat]);
      this.searchModel.set({ ...EMPTY_SEARCH_MODEL });
      this.searchResults.set([]);
      this.closeNewChatModal();
      this.router.navigate(['chats', chat.id]);
    } catch (err) {
      console.error(err);
    }
  }

  async createGroupChat() {
    const name = this.groupModel().name.trim();
    const selectedMembers = this.selectedGroupMembers();

    if (!name || selectedMembers.length === 0 || this.isCreatingGroup()) {
      return;
    }

    this.isCreatingGroup.set(true);
    this.groupCreateError.set(null);

    try {
      const chat = await this.chatService.createGroupChat({
        name,
        description: this.groupModel().description.trim(),
        imageUrl: this.groupModel().imageUrl.trim(),
        memberIds: selectedMembers.map((member) => member.id),
      });

      this.chats.update((old) => [...old, chat]);
      this.closeNewGroupModal();
      this.router.navigate(['chats', chat.id]);
    } catch (err) {
      console.error(err);
      this.groupCreateError.set(this.getErrorMessage(err, 'Could not create group chat.'));
    } finally {
      this.isCreatingGroup.set(false);
    }
  }

  async ngOnInit() {
    this.chatService.loadMyChats();
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

  toggleGroupMember(user: UserCandidate) {
    if (this.isGroupMemberSelected(user.id)) {
      this.selectedGroupMembers.update((members) =>
        members.filter((member) => member.id !== user.id),
      );
      return;
    }

    this.selectedGroupMembers.update((members) => [...members, user]);
  }

  removeGroupMember(userId: string) {
    this.selectedGroupMembers.update((members) => members.filter((member) => member.id !== userId));
  }

  isGroupMemberSelected(userId: string): boolean {
    return this.selectedGroupMembers().some((member) => member.id === userId);
  }

  canCreateGroup(): boolean {
    return this.groupModel().name.trim().length > 0 && this.selectedGroupMembers().length > 0;
  }

  getCandidateFullName(user: UserCandidate): string {
    return user.fullName || `${user.firstName} ${user.lastName}`.trim();
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

  getGroupFallbackInitial(chat: Chat): string {
    return this.chatService.getChatTitle(chat).charAt(0).toUpperCase();
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

  private resetUserSearch() {
    this.searchModel.set({ ...EMPTY_SEARCH_MODEL });
    this.searchResults.set([]);
    this.isSearching.set(false);
  }

  private resetGroupForm() {
    this.groupModel.set({ ...EMPTY_GROUP_MODEL });
    this.selectedGroupMembers.set([]);
    this.groupCreateError.set(null);
  }

  private userToCandidate(user: User): UserCandidate {
    return {
      id: user.id,
      username: user.username,
      firstName: user.firstName,
      lastName: user.lastName,
      fullName: `${user.firstName} ${user.lastName}`.trim(),
      phoneNumber: user.phoneNumber,
      pfpUrl: user.pfpUrl,
      lastActiveAt: user.lastActiveAt,
    };
  }

  private memberToCandidate(member: ChatMember): UserCandidate {
    return {
      id: member.userId,
      username: member.username,
      firstName: member.firstName,
      lastName: member.lastName,
      fullName: member.fullName,
      phoneNumber: member.phoneNumber,
      pfpUrl: member.pfpUrl,
      lastActiveAt: member.lastActiveAt,
    };
  }

  private matchesSearchCriteria(user: UserCandidate, search: UserSearchModel): boolean {
    const content = search.content.trim().toLowerCase();

    if (content) {
      const values = [
        user.username,
        user.firstName,
        user.lastName,
        this.getCandidateFullName(user),
        user.phoneNumber ?? '',
      ];

      if (!values.some((value) => value.toLowerCase().includes(content))) {
        return false;
      }
    }

    if (!this.matchesProfilePictureFilter(user, search.profilePicture)) {
      return false;
    }

    return this.matchesLastActiveFilter(user.lastActiveAt, search.lastActiveFrom, search.lastActiveTo);
  }

  private matchesProfilePictureFilter(
    user: UserCandidate,
    profilePicture: ProfilePictureFilter,
  ): boolean {
    if (profilePicture === 'ANY') {
      return true;
    }

    const hasProfilePicture =
      Boolean(user.pfpUrl?.trim()) && user.pfpUrl !== DEFAULT_PROFILE_PICTURE_URL;

    return profilePicture === 'SET' ? hasProfilePicture : !hasProfilePicture;
  }

  private matchesLastActiveFilter(
    lastActiveAt: Date | string | null | undefined,
    lastActiveFrom: string,
    lastActiveTo: string,
  ): boolean {
    if (!lastActiveFrom && !lastActiveTo) {
      return true;
    }

    if (!lastActiveAt) {
      return false;
    }

    const lastActiveDate = new Date(lastActiveAt);

    if (Number.isNaN(lastActiveDate.getTime())) {
      return false;
    }

    if (lastActiveFrom) {
      const fromDate = new Date(`${lastActiveFrom}T00:00:00`);

      if (lastActiveDate < fromDate) {
        return false;
      }
    }

    if (lastActiveTo) {
      const toDate = new Date(`${lastActiveTo}T23:59:59.999`);

      if (lastActiveDate > toDate) {
        return false;
      }
    }

    return true;
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
