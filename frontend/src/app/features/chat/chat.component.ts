import { formatDate } from '@angular/common';
import {
  afterNextRender,
  Component,
  DestroyRef,
  computed,
  effect,
  ElementRef,
  inject,
  Injector,
  signal,
  ViewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { form, FormField, FormRoot, required } from '@angular/forms/signals';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { ChatService } from '@core/services/chat.service';
import { StompService } from '@core/services/stomp.service';
import { UserService } from '@core/services/user.service';
import { ModalComponent } from '@shared/components/modal/modal.component';
import { SidebarComponent } from '@shared/components/sidebar/sidebar.component';
import {
  Chat,
  ChatMember,
  ChatEvent,
  Message,
  MessageDeliveryStatus,
  MessageReceipt,
  MessageReactionSummary,
  MessageReactionType,
  MessageReference,
  MessageStatus,
  User,
} from '@shared/types/api.types';
import {
  LucideForward,
  LucideReply,
  LucideSend,
  LucideStar,
  LucideUsers,
  LucideX,
} from '@lucide/angular';
import { StompSubscription } from '@stomp/stompjs';
import { forkJoin, switchMap, tap } from 'rxjs';

interface GroupSettingsModel {
  name: string;
  description: string;
  imageUrl: string;
}

interface GroupMemberSearchModel {
  content: string;
}

interface AddableGroupMember {
  id: string;
  username: string;
  firstName: string;
  lastName: string;
  fullName: string;
  phoneNumber?: string | null;
  pfpUrl: string;
  lastActiveAt?: Date | string | null;
}

const EMPTY_GROUP_SETTINGS_MODEL: GroupSettingsModel = {
  name: '',
  description: '',
  imageUrl: '',
};

@Component({
  templateUrl: './chat.component.html',
  imports: [
    SidebarComponent,
    ModalComponent,
    FormRoot,
    FormField,
    LucideForward,
    LucideReply,
    LucideSend,
    LucideStar,
    LucideUsers,
    LucideX,
  ],
})
export class ChatComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly chatService = inject(ChatService);
  private readonly userService = inject(UserService);
  private readonly authService = inject(AuthService);
  private readonly stompService = inject(StompService);
  private readonly destroyRef = inject(DestroyRef);

  @ViewChild('messagesContainer')
  private messagesContainer?: ElementRef<HTMLElement>;
  private injector = inject(Injector);

  constructor() {
    effect(() => {
      this.messages();

      afterNextRender(
        () => {
          this.scrollToBottom();
        },
        {
          injector: this.injector,
        },
      );
    });

    effect((onCleanup) => {
      const content = this.groupMemberSearchModel().content.trim();
      const DEBOUNCE_TIMER = 300;

      if (!this.isGroupSettingsModalOpen() || !this.isCurrentUserGroupAdmin() || !content) {
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

  private scrollToBottom() {
    const el = this.messagesContainer?.nativeElement;

    if (!el) {
      return;
    }

    el.scrollTop = el.scrollHeight;
  }
  currentUser = signal<User | null>(this.userService.getLoggedInUser());

  chat = signal<Chat | null>(null);
  chatTitle = signal<string>('');
  chatImage = signal<string>('');
  selectedChatId = signal<string | null>(null);
  messages = signal<Message[]>([]);
  messageReceipts = signal<MessageReceipt[]>([]);
  replyingTo = signal<Message | null>(null);
  forwardingMessage = signal<Message | null>(null);
  isForwardModalOpen = signal(false);
  isUserProfileModalOpen = signal(false);
  isGroupSettingsModalOpen = signal(false);
  selectedProfileUser = signal<User | null>(null);
  userProfileError = signal<string | null>(null);
  groupSettingsError = signal<string | null>(null);
  addGroupMemberError = signal<string | null>(null);
  isSavingGroupSettings = signal(false);
  isUpdatingGroupMember = signal(false);
  isSearchingGroupMembers = signal(false);
  groupMemberSearchResults = signal<User[]>([]);

  groupSettingsModel = signal<GroupSettingsModel>({ ...EMPTY_GROUP_SETTINGS_MODEL });
  groupSettingsForm = form(this.groupSettingsModel);
  groupMemberSearchModel = signal<GroupMemberSearchModel>({ content: '' });
  groupMemberSearchForm = form(this.groupMemberSearchModel);

  currentGroupMembers = computed(() => {
    const chat = this.chat();

    return chat?.type === 'GROUP' ? chat.members : [];
  });

  isCurrentUserGroupAdmin = computed(() => {
    const currentUserId = this.currentUser()?.id;

    return this.currentGroupMembers().some(
      (member) => member.userId === currentUserId && member.role === 'ADMIN',
    );
  });

  availableContactMembers = computed(() => {
    const currentUserId = this.currentUser()?.id;
    const currentMemberIds = new Set(this.currentGroupMembers().map((member) => member.userId));
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
    const currentMemberIds = new Set(this.currentGroupMembers().map((member) => member.userId));
    const contactIds = new Set(this.availableContactMembers().map((member) => member.id));

    return this.groupMemberSearchResults()
      .filter((user) => !currentMemberIds.has(user.id) && !contactIds.has(user.id))
      .map((user) => this.userToAddable(user));
  });

  readonly reactionOptions: { type: MessageReactionType; emoji: string; label: string }[] = [
    { type: 'HEART', emoji: '❤', label: 'Heart' },
    { type: 'LIKE', emoji: '👍', label: 'Like' },
    { type: 'LAUGH', emoji: '😂', label: 'Laugh' },
    { type: 'CRY', emoji: '😢', label: 'Cry' },
  ];

  messageModel = signal({
    content: '',
  });

  messageForm = form(this.messageModel, (path) => {
    required(path.content, {
      message: 'Message cannot be empty',
    });
  });

  private chatEventSubscription: StompSubscription | null = null;

  ngOnInit() {
    this.setupLiveChat();

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
        tap(({ chat, messages, messageReceipts }) => {
          this.chat.set(chat);
          this.selectedChatId.set(chat.id);
          this.messages.set(messages.map((message) => this.normalizeMessage(message)));
          this.messageReceipts.set(messageReceipts);
          this.cancelReply();
          this.closeForwardModal();
          this.closeGroupSettingsModal();

          this.setupInfo(chat);
        }),
        switchMap(({ chat }) =>
          this.chatService.markAsRead(chat.id).pipe(
            tap(() => {
              this.chatService.markChatAsReadLocally(chat.id);
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        error: (err) => {
          console.error(err);

          if (err.status === 404) {
            this.router.navigate(['/']);
          }
        },
      });

    this.destroyRef.onDestroy(() => {
      this.chatEventSubscription?.unsubscribe();
    });
  }

  private setupInfo(chat: Chat) {
    if (!chat) {
      throw new Error('error loading chat');
    }

    if (chat.type === 'DIRECT') {
      const recipient = chat.members.filter((m) => m.userId !== this.currentUser()?.id)[0];
      this.chatTitle.set(recipient.fullName);
      this.chatImage.set(recipient.pfpUrl);
      return;
    }

    if (chat.type === 'GROUP') {
      this.chatTitle.set(chat.name ?? 'Group Chat');
      this.chatImage.set(chat.imageUrl ?? '');
    }
  }

  getDirectRecipient(): ChatMember | null {
    const chat = this.chat();

    if (!chat || chat.type !== 'DIRECT') {
      return null;
    }

    return chat.members.find((member) => member.userId !== this.currentUser()?.id) ?? null;
  }

  openUserProfile(userId: string): void {
    this.isUserProfileModalOpen.set(true);
    this.selectedProfileUser.set(null);
    this.userProfileError.set(null);

    this.userService
      .getUserById(userId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (user) => {
          this.selectedProfileUser.set(user);
        },
        error: (err) => {
          console.error('Could not load user profile', err);
          this.userProfileError.set('Could not load user profile.');
        },
      });
  }

  closeUserProfileModal(): void {
    this.isUserProfileModalOpen.set(false);
    this.selectedProfileUser.set(null);
    this.userProfileError.set(null);
  }

  openGroupSettingsModal(): void {
    const chat = this.chat();

    if (!chat || chat.type !== 'GROUP') {
      return;
    }

    this.groupSettingsModel.set({
      name: chat.name ?? '',
      description: chat.description ?? '',
      imageUrl: chat.imageUrl ?? '',
    });
    this.groupMemberSearchModel.set({ content: '' });
    this.groupMemberSearchResults.set([]);
    this.groupSettingsError.set(null);
    this.addGroupMemberError.set(null);
    this.isGroupSettingsModalOpen.set(true);
  }

  closeGroupSettingsModal(): void {
    this.isGroupSettingsModalOpen.set(false);
    this.groupSettingsError.set(null);
    this.addGroupMemberError.set(null);
    this.groupMemberSearchModel.set({ content: '' });
    this.groupMemberSearchResults.set([]);
    this.isSearchingGroupMembers.set(false);
  }

  saveGroupSettings(): void {
    const chat = this.chat();
    const name = this.groupSettingsModel().name.trim();

    if (!chat || chat.type !== 'GROUP' || !this.isCurrentUserGroupAdmin() || !name) {
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
          this.applyUpdatedChat(updatedChat);
          this.reloadCurrentMessages();
          this.isSavingGroupSettings.set(false);
        },
        error: (err) => {
          console.error('Could not update group', err);
          this.groupSettingsError.set(this.getErrorMessage(err, 'Could not update group.'));
          this.isSavingGroupSettings.set(false);
        },
      });
  }

  addGroupMember(user: AddableGroupMember): void {
    const chat = this.chat();

    if (!chat || chat.type !== 'GROUP' || !this.isCurrentUserGroupAdmin()) {
      return;
    }

    this.isUpdatingGroupMember.set(true);
    this.addGroupMemberError.set(null);

    this.chatService
      .addGroupMembers(chat.id, [user.id])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updatedChat) => {
          this.applyUpdatedChat(updatedChat);
          this.reloadCurrentMessages();
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

    if (!chat || chat.type !== 'GROUP' || !this.canRemoveGroupMember(member)) {
      return;
    }

    this.isUpdatingGroupMember.set(true);
    this.addGroupMemberError.set(null);

    this.chatService
      .removeGroupMember(chat.id, member.memberId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updatedChat) => {
          this.applyUpdatedChat(updatedChat);
          this.reloadCurrentMessages();
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

    if (!chat || chat.type !== 'GROUP' || !this.canChangeGroupMemberRole(member)) {
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
          this.applyUpdatedChat(updatedChat);
          this.reloadCurrentMessages();
          this.isUpdatingGroupMember.set(false);
        },
        error: (err) => {
          console.error('Could not update group member role', err);
          this.addGroupMemberError.set(this.getErrorMessage(err, 'Could not update group role.'));
          this.isUpdatingGroupMember.set(false);
        },
      });
  }

  canSaveGroupSettings(): boolean {
    return (
      this.isCurrentUserGroupAdmin() &&
      this.groupSettingsModel().name.trim().length > 0 &&
      !this.isSavingGroupSettings()
    );
  }

  canChangeGroupMemberRole(member: ChatMember): boolean {
    if (!this.isCurrentUserGroupAdmin() || member.userId === this.currentUser()?.id) {
      return false;
    }

    return member.role !== 'ADMIN' || this.activeGroupAdminCount() > 1;
  }

  canRemoveGroupMember(member: ChatMember): boolean {
    if (!this.isCurrentUserGroupAdmin() || member.userId === this.currentUser()?.id) {
      return false;
    }

    return member.role !== 'ADMIN' || this.activeGroupAdminCount() > 1;
  }

  activeGroupAdminCount(): number {
    return this.currentGroupMembers().filter((member) => member.role === 'ADMIN').length;
  }

  getAddableMemberName(user: AddableGroupMember): string {
    return user.fullName || `${user.firstName} ${user.lastName}`.trim();
  }

  isOwnMessage(message: Message): boolean {
    return message.sender.userId === this.currentUser()?.id;
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

  private setupLiveChat(): void {
    console.log('setupLiveChat called');

    this.stompService.connect(this.authService.getToken(), () => {
      console.log('STOMP connected callback fired');

      if (this.chatEventSubscription) {
        console.log('Already subscribed to chat events');
        return;
      }

      this.chatEventSubscription = this.stompService.subscribeJson<ChatEvent>(
        '/user/queue/chat-events',
        (event) => {
          console.log('CHAT EVENT RECEIVED:', event);
          this.handleChatEvent(event);
        },
      );

      console.log('Subscription result:', this.chatEventSubscription);
    });
  }

  private handleChatEvent(event: ChatEvent): void {
    if (event.type === 'MESSAGE_CREATED') {
      this.handleMessageCreated(event);
      return;
    }

    if (event.type === 'MESSAGE_STATUSES_UPDATED') {
      this.handleMessageStatusesUpdated(event.messageStatuses ?? []);
      return;
    }

    if (event.type === 'MESSAGE_REACTIONS_UPDATED') {
      this.handleMessageReactionsUpdated(event.messageId, event.messageReactions ?? []);
    }
  }

  private handleMessageCreated(event: ChatEvent): void {
    const createdMessage = event.message;

    if (!createdMessage) {
      return;
    }

    const selectedChatId = this.selectedChatId();

    if (event.chatId !== selectedChatId) {
      this.chatService.updateChatUnreadCount(event.chatId, event.unreadCount);
      return;
    }

    const normalizedMessage = this.normalizeMessage(createdMessage);
    const messageAlreadyExist = this.messages().some((message) => message.id === normalizedMessage.id);

    if (messageAlreadyExist) {
      return;
    }

    this.messages.update((old) => [...old, normalizedMessage]);

    if (!this.isOwnMessage(normalizedMessage)) {
      this.markCurrentChatAsRead();
    }
  }

  private handleMessageStatusesUpdated(messageStatuses: MessageStatus[]): void {
    if (messageStatuses.length === 0) {
      return;
    }

    const statusesByMessageId = new Map(
      messageStatuses.map((messageStatus) => [messageStatus.messageId, messageStatus.status]),
    );

    this.messages.update((messages) =>
      messages.map((message) => {
        const deliveryStatus = statusesByMessageId.get(message.id);

        return deliveryStatus ? { ...message, deliveryStatus } : message;
      }),
    );
  }

  private handleMessageReactionsUpdated(
    messageId: string | undefined,
    reactions: MessageReactionSummary[],
  ): void {
    if (!messageId) {
      return;
    }

    this.messages.update((messages) =>
      messages.map((message) => (message.id === messageId ? { ...message, reactions } : message)),
    );
  }

  sendMessage(): void {
    const chatId = this.selectedChatId();
    const content = this.messageModel().content.trim();
    const replyToMessageId = this.replyingTo()?.id;

    if (!chatId || !content) {
      return;
    }

    this.stompService.publishJson('/app/chat.send', {
      chatId: chatId,
      content: content,
      replyToMessageId,
    });

    this.messageModel.set({
      content: '',
    });
    this.cancelReply();
  }

  startReply(message: Message): void {
    this.replyingTo.set(message);
  }

  cancelReply(): void {
    this.replyingTo.set(null);
  }

  openForwardModal(message: Message): void {
    this.forwardingMessage.set(message);
    this.isForwardModalOpen.set(true);
  }

  closeForwardModal(): void {
    this.isForwardModalOpen.set(false);
    this.forwardingMessage.set(null);
  }

  forwardMessage(targetChat: Chat): void {
    const message = this.forwardingMessage();

    if (!message) {
      return;
    }

    this.stompService.publishJson('/app/chat.send', {
      chatId: targetChat.id,
      content: message.content,
      forwardedFromMessageId: message.id,
    });

    this.closeForwardModal();
  }

  reactToMessage(message: Message, type: MessageReactionType): void {
    this.stompService.publishJson('/app/chat.react', {
      messageId: message.id,
      type,
    });
  }

  toggleStarredMessage(message: Message): void {
    const chatId = this.selectedChatId();

    if (!chatId) {
      return;
    }

    this.chatService
      .toggleStarredMessage(chatId, message.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (update) => {
          this.messages.update((messages) =>
            messages.map((candidate) =>
              candidate.id === update.messageId
                ? { ...candidate, starredByMe: update.starred }
                : candidate,
            ),
          );
        },
        error: (err) => console.error('Could not update starred message', err),
      });
  }

  getForwardableChats(): Chat[] {
    const selectedChatId = this.selectedChatId();

    return this.chatService.chats().filter((chat) => chat.id !== selectedChatId);
  }

  getReplyReference(message: Message): MessageReference | null {
    return message.replyTo ?? this.findMessageReference(message.replyToMessageId);
  }

  getForwardedReference(message: Message): MessageReference | null {
    return message.forwardedFrom ?? this.findMessageReference(message.forwardedFromMessageId);
  }

  shouldRenderContent(message: Message): boolean {
    const forwardedReference = this.getForwardedReference(message);

    return !forwardedReference || forwardedReference.content !== message.content;
  }

  emojiForReaction(type: MessageReactionType): string {
    return this.reactionOptions.find((reaction) => reaction.type === type)?.emoji ?? '';
  }

  labelForReaction(type: MessageReactionType): string {
    return this.reactionOptions.find((reaction) => reaction.type === type)?.label ?? type;
  }

  private markCurrentChatAsRead(): void {
    const chatId = this.selectedChatId();

    if (!chatId) {
      return;
    }

    this.chatService
      .markAsRead(chatId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        error: (err) => console.error('Could not mark chat as read', err),
      });
  }

  getMessageStatusLabel(status: MessageDeliveryStatus): string {
    if (status === 'READ') {
      return 'Read';
    }

    if (status === 'DELIVERED') {
      return 'Delivered';
    }

    return 'Sent';
  }

  private findMessageReference(messageId: string | null | undefined): MessageReference | null {
    if (!messageId) {
      return null;
    }

    const message = this.messages().find((candidate) => candidate.id === messageId);

    if (!message) {
      return null;
    }

    return {
      id: message.id,
      senderFullName: message.sender.fullName,
      content: message.content,
    };
  }

  private applyUpdatedChat(updatedChat: Chat): void {
    this.chat.set(updatedChat);
    this.setupInfo(updatedChat);

    this.chatService.chats.update((chats) =>
      chats.map((chat) => (chat.id === updatedChat.id ? updatedChat : chat)),
    );
  }

  private reloadCurrentMessages(): void {
    const chatId = this.selectedChatId();

    if (!chatId) {
      return;
    }

    this.chatService
      .getMessagesByChatId(chatId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (messages) => {
          this.messages.set(messages.map((message) => this.normalizeMessage(message)));
        },
        error: (err) => console.error('Could not reload messages', err),
      });
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

  private normalizeMessage(message: Message): Message {
    return {
      ...message,
      reactions: message.reactions ?? [],
      starredByMe: message.starredByMe ?? false,
      systemMessage: message.systemMessage ?? false,
    };
  }
}
