import { formatDate } from '@angular/common';
import {
  Component,
  DestroyRef,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { FormField, form } from '@angular/forms/signals';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ChatService } from '@core/services/chat.service';
import { UserService } from '@core/services/user.service';
import { ModalComponent } from '@shared/components/modal/modal.component';
import { Chat, ChatMember, User } from '@shared/types/api.types';
import { LucideCopy, LucideLink, LucideRefreshCw, LucideTrash2 } from '@lucide/angular';
import { AddableGroupMember, GroupSettingsModel } from '../../chat.types';

const EMPTY_GROUP_SETTINGS_MODEL: GroupSettingsModel = {
  name: '',
  description: '',
  imageUrl: '',
};

@Component({
  selector: 'app-group-settings-modal',
  templateUrl: './group-settings-modal.component.html',
  imports: [ModalComponent, FormField, LucideCopy, LucideLink, LucideRefreshCw, LucideTrash2],
})
export class GroupSettingsModalComponent {
  private readonly chatService = inject(ChatService);
  private readonly userService = inject(UserService);
  private readonly destroyRef = inject(DestroyRef);

  chat = input.required<Chat>();
  title = input.required<string>();
  currentUserId = input.required<string | null>();

  close = output<void>();
  openUserProfile = output<string>();
  chatUpdated = output<Chat>();

  groupSettingsError = signal<string | null>(null);
  addGroupMemberError = signal<string | null>(null);
  isSavingGroupSettings = signal(false);
  isUpdatingGroupMember = signal(false);
  isSearchingGroupMembers = signal(false);
  groupMemberSearchResults = signal<User[]>([]);
  groupInviteToken = signal<string | null>(null);
  groupInviteGeneratedAt = signal<Date | string | null>(null);
  groupInviteError = signal<string | null>(null);
  groupInviteCopied = signal(false);
  isLoadingGroupInvite = signal(false);
  isGeneratingGroupInvite = signal(false);
  isRevokingGroupInvite = signal(false);

  groupSettingsModel = signal<GroupSettingsModel>({ ...EMPTY_GROUP_SETTINGS_MODEL });
  groupSettingsForm = form(this.groupSettingsModel);
  groupMemberSearchModel = signal({ content: '' });
  groupMemberSearchForm = form(this.groupMemberSearchModel);

  members = computed(() => {
    const chat = this.chat();

    return chat.type === 'GROUP' ? chat.members : [];
  });

  isCurrentUserGroupAdmin = computed(() =>
    this.members().some(
      (member) => member.userId === this.currentUserId() && member.role === 'ADMIN',
    ),
  );

  canSaveGroupSettings = computed(
    () =>
      this.isCurrentUserGroupAdmin() &&
      this.groupSettingsModel().name.trim().length > 0 &&
      !this.isSavingGroupSettings(),
  );

  groupInviteUrl = computed(() => {
    const token = this.groupInviteToken();

    if (!token) {
      return '';
    }

    return `${window.location.origin}/invite/${encodeURIComponent(token)}`;
  });

  availableContactMembers = computed(() => {
    const currentUserId = this.currentUserId();
    const currentMemberIds = new Set(this.members().map((member) => member.userId));
    const usersById = new Map<string, AddableGroupMember>();
    const search = this.groupMemberSearchModel().content.trim().toLowerCase();

    for (const chat of this.chatService.chats()) {
      for (const member of chat.members) {
        if (
          member.userId === currentUserId ||
          currentMemberIds.has(member.userId) ||
          usersById.has(member.userId)
        ) {
          continue;
        }

        const candidate = this.memberToAddable(member);

        if (!search || this.matchesAddableMemberSearch(candidate, search)) {
          usersById.set(candidate.id, candidate);
        }
      }
    }

    return Array.from(usersById.values()).sort((a, b) =>
      this.getAddableMemberName(a).localeCompare(this.getAddableMemberName(b)),
    );
  });

  newGroupMemberSearchResults = computed(() => {
    const currentMemberIds = new Set(this.members().map((member) => member.userId));
    const contactIds = new Set(this.availableContactMembers().map((member) => member.id));

    return this.groupMemberSearchResults()
      .filter((user) => !currentMemberIds.has(user.id) && !contactIds.has(user.id))
      .map((user) => this.userToAddable(user));
  });

  constructor() {
    effect((onCleanup) => {
      const content = this.groupMemberSearchModel().content.trim();
      const DEBOUNCE_TIMER = 300;

      if (!content || !this.isCurrentUserGroupAdmin()) {
        this.groupMemberSearchResults.set([]);
        this.isSearchingGroupMembers.set(false);
        return;
      }

      this.isSearchingGroupMembers.set(true);
      let subscription: { unsubscribe: () => void } | null = null;

      const timeoutId = setTimeout(() => {
        subscription = this.userService.searchUsers(content).subscribe({
          next: (users) => {
            this.groupMemberSearchResults.set(users);
            this.isSearchingGroupMembers.set(false);
          },
          error: (err) => {
            console.error('Could not search users', err);
            this.groupMemberSearchResults.set([]);
            this.isSearchingGroupMembers.set(false);
          },
        });
      }, DEBOUNCE_TIMER);

      onCleanup(() => {
        clearTimeout(timeoutId);
        subscription?.unsubscribe();
      });
    });
  }

  ngOnInit(): void {
    this.setupGroupSettingsForm(this.chat());

    if (this.isCurrentUserGroupAdmin()) {
      this.loadGroupInviteLink();
    }
  }

  saveGroupSettings(): void {
    const chat = this.chat();
    const name = this.groupSettingsModel().name.trim();

    if (chat.type !== 'GROUP' || !this.isCurrentUserGroupAdmin() || !name) {
      return;
    }

    this.isSavingGroupSettings.set(true);
    this.groupSettingsError.set(null);

    this.chatService
      .updateGroupChat(chat.id, {
        name,
        description: this.groupSettingsModel().description.trim(),
        imageUrl: this.groupSettingsModel().imageUrl.trim(),
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updatedChat) => {
          this.handleUpdatedChat(updatedChat);
          this.isSavingGroupSettings.set(false);
        },
        error: (err) => {
          console.error('Could not update group', err);
          this.groupSettingsError.set(this.getErrorMessage(err, 'Could not update group.'));
          this.isSavingGroupSettings.set(false);
        },
      });
  }

  generateGroupInviteLink(): void {
    const chat = this.chat();

    if (chat.type !== 'GROUP' || !this.isCurrentUserGroupAdmin()) {
      return;
    }

    this.isGeneratingGroupInvite.set(true);
    this.groupInviteError.set(null);
    this.groupInviteCopied.set(false);

    this.chatService
      .generateGroupInviteLink(chat.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (inviteLink) => {
          this.groupInviteToken.set(inviteLink.token ?? null);
          this.groupInviteGeneratedAt.set(inviteLink.generatedAt ?? null);
          this.isGeneratingGroupInvite.set(false);
        },
        error: (err) => {
          console.error('Could not generate invite link', err);
          this.groupInviteError.set(this.getErrorMessage(err, 'Could not generate invite link.'));
          this.isGeneratingGroupInvite.set(false);
        },
      });
  }

  revokeGroupInviteLink(): void {
    const chat = this.chat();

    if (chat.type !== 'GROUP' || !this.isCurrentUserGroupAdmin() || !this.groupInviteToken()) {
      return;
    }

    this.isRevokingGroupInvite.set(true);
    this.groupInviteError.set(null);
    this.groupInviteCopied.set(false);

    this.chatService
      .revokeGroupInviteLink(chat.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.groupInviteToken.set(null);
          this.groupInviteGeneratedAt.set(null);
          this.isRevokingGroupInvite.set(false);
        },
        error: (err) => {
          console.error('Could not revoke invite link', err);
          this.groupInviteError.set(this.getErrorMessage(err, 'Could not revoke invite link.'));
          this.isRevokingGroupInvite.set(false);
        },
      });
  }

  copyGroupInviteLink(): void {
    const inviteUrl = this.groupInviteUrl();

    if (!inviteUrl) {
      return;
    }

    if (!navigator.clipboard) {
      this.groupInviteError.set('Could not copy invite link.');
      return;
    }

    navigator.clipboard
      .writeText(inviteUrl)
      .then(() => {
        this.groupInviteError.set(null);
        this.groupInviteCopied.set(true);
        setTimeout(() => this.groupInviteCopied.set(false), 1500);
      })
      .catch((err) => {
        console.error('Could not copy invite link', err);
        this.groupInviteError.set('Could not copy invite link.');
      });
  }

  addGroupMember(user: AddableGroupMember): void {
    const chat = this.chat();

    if (chat.type !== 'GROUP' || !this.isCurrentUserGroupAdmin()) {
      return;
    }

    this.isUpdatingGroupMember.set(true);
    this.addGroupMemberError.set(null);

    this.chatService
      .addGroupMembers(chat.id, [user.id])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updatedChat) => {
          this.handleUpdatedChat(updatedChat);
          this.groupMemberSearchResults.update((users) =>
            users.filter((candidate) => candidate.id !== user.id),
          );
          this.isUpdatingGroupMember.set(false);
        },
        error: (err) => {
          console.error('Could not add group member', err);
          this.addGroupMemberError.set(this.getErrorMessage(err, 'Could not add group member.'));
          this.isUpdatingGroupMember.set(false);
        },
      });
  }

  removeGroupMember(member: ChatMember): void {
    const chat = this.chat();

    if (chat.type !== 'GROUP' || !this.canRemoveGroupMember(member)) {
      return;
    }

    this.isUpdatingGroupMember.set(true);
    this.addGroupMemberError.set(null);

    this.chatService
      .removeGroupMember(chat.id, member.memberId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updatedChat) => {
          this.handleUpdatedChat(updatedChat);
          this.isUpdatingGroupMember.set(false);
        },
        error: (err) => {
          console.error('Could not remove group member', err);
          this.addGroupMemberError.set(this.getErrorMessage(err, 'Could not remove group member.'));
          this.isUpdatingGroupMember.set(false);
        },
      });
  }

  toggleGroupMemberRole(member: ChatMember): void {
    const chat = this.chat();

    if (chat.type !== 'GROUP' || !this.canChangeGroupMemberRole(member)) {
      return;
    }

    const role = member.role === 'ADMIN' ? 'MEMBER' : 'ADMIN';
    this.isUpdatingGroupMember.set(true);
    this.addGroupMemberError.set(null);

    this.chatService
      .updateGroupMemberRole(chat.id, member.memberId, role)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updatedChat) => {
          this.handleUpdatedChat(updatedChat);
          this.isUpdatingGroupMember.set(false);
        },
        error: (err) => {
          console.error('Could not update group member role', err);
          this.addGroupMemberError.set(this.getErrorMessage(err, 'Could not update group role.'));
          this.isUpdatingGroupMember.set(false);
        },
      });
  }

  formatInviteGeneratedAt(value: Date | string | null): string {
    if (!value) {
      return '';
    }

    return formatDate(new Date(value), 'dd.MM.yyyy. HH:mm', 'en-US');
  }

  canChangeGroupMemberRole(member: ChatMember): boolean {
    if (!this.isCurrentUserGroupAdmin() || member.userId === this.currentUserId()) {
      return false;
    }

    return member.role !== 'ADMIN' || this.activeGroupAdminCount() > 1;
  }

  canRemoveGroupMember(member: ChatMember): boolean {
    if (!this.isCurrentUserGroupAdmin() || member.userId === this.currentUserId()) {
      return false;
    }

    return member.role !== 'ADMIN' || this.activeGroupAdminCount() > 1;
  }

  getAddableMemberName(user: AddableGroupMember): string {
    return user.fullName || `${user.firstName} ${user.lastName}`.trim();
  }

  private setupGroupSettingsForm(chat: Chat): void {
    this.groupSettingsModel.set({
      name: chat.name ?? '',
      description: chat.description ?? '',
      imageUrl: chat.imageUrl ?? '',
    });
  }

  private loadGroupInviteLink(): void {
    const chat = this.chat();

    if (chat.type !== 'GROUP') {
      return;
    }

    this.isLoadingGroupInvite.set(true);
    this.groupInviteError.set(null);

    this.chatService
      .getGroupInviteLink(chat.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (inviteLink) => {
          this.groupInviteToken.set(inviteLink.token ?? null);
          this.groupInviteGeneratedAt.set(inviteLink.generatedAt ?? null);
          this.isLoadingGroupInvite.set(false);
        },
        error: (err) => {
          console.error('Could not load invite link', err);
          this.groupInviteError.set(this.getErrorMessage(err, 'Could not load invite link.'));
          this.isLoadingGroupInvite.set(false);
        },
      });
  }

  private handleUpdatedChat(updatedChat: Chat): void {
    this.setupGroupSettingsForm(updatedChat);
    this.chatUpdated.emit(updatedChat);
  }

  private activeGroupAdminCount(): number {
    return this.members().filter((member) => member.role === 'ADMIN').length;
  }

  private userToAddable(user: User): AddableGroupMember {
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

  private memberToAddable(member: ChatMember): AddableGroupMember {
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

  private matchesAddableMemberSearch(user: AddableGroupMember, search: string): boolean {
    return [
      user.username,
      user.firstName,
      user.lastName,
      this.getAddableMemberName(user),
      user.phoneNumber ?? '',
    ].some((value) => value.toLowerCase().includes(search));
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
