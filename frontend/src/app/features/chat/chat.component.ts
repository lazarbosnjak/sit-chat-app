import { formatDate } from '@angular/common';
import {
  afterNextRender,
  Component,
  DestroyRef,
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
import { LucideForward, LucideReply, LucideSend, LucideStar, LucideX } from '@lucide/angular';
import { StompSubscription } from '@stomp/stompjs';
import { forkJoin, switchMap, tap } from 'rxjs';

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

  private normalizeMessage(message: Message): Message {
    return {
      ...message,
      reactions: message.reactions ?? [],
      starredByMe: message.starredByMe ?? false,
    };
  }
}
