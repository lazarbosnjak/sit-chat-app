import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { ChatService } from '@core/services/chat.service';
import { StompService } from '@core/services/stomp.service';
import { UserService } from '@core/services/user.service';
import { SidebarComponent } from '@shared/components/sidebar/sidebar.component';
import {
  Chat,
  ChatMember,
  ChatEvent,
  Message,
  MessageReceipt,
  MessageReactionSummary,
  MessageReactionType,
  MessageStatus,
  User,
} from '@shared/types/api.types';
import { StompSubscription } from '@stomp/stompjs';
import { forkJoin, switchMap, tap } from 'rxjs';
import { ChatHeaderComponent } from './components/chat-header/chat-header.component';
import { ForwardMessageModalComponent } from './components/forward-message-modal/forward-message-modal.component';
import { GroupSettingsModalComponent } from './components/group-settings-modal/group-settings-modal.component';
import { MessageComposerComponent } from './components/message-composer/message-composer.component';
import { MessageListComponent } from './components/message-list/message-list.component';
import { UserProfileModalComponent } from './components/user-profile-modal/user-profile-modal.component';
import { ReactionOption } from './chat.types';

@Component({
  templateUrl: './chat.component.html',
  imports: [
    SidebarComponent,
    ChatHeaderComponent,
    MessageListComponent,
    MessageComposerComponent,
    ForwardMessageModalComponent,
    UserProfileModalComponent,
    GroupSettingsModalComponent,
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
  isRecordingAudio = signal(false);
  isUploadingVoiceMessage = signal(false);
  audioRecordingError = signal<string | null>(null);
  audioObjectUrls = signal<Record<string, string>>({});

  private mediaRecorder: MediaRecorder | null = null;
  private recordingStream: MediaStream | null = null;
  private audioChunks: Blob[] = [];
  private recordingStartedAt = 0;
  private loadingAudioIds = new Set<string>();

  readonly reactionOptions: ReactionOption[] = [
    { type: 'HEART', emoji: '❤', label: 'Heart' },
    { type: 'LIKE', emoji: '👍', label: 'Like' },
    { type: 'LAUGH', emoji: '😂', label: 'Laugh' },
    { type: 'CRY', emoji: '😢', label: 'Cry' },
  ];

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
      this.stopRecordingStream();
      Object.values(this.audioObjectUrls()).forEach((url) => URL.revokeObjectURL(url));
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

    this.isGroupSettingsModalOpen.set(true);
  }

  closeGroupSettingsModal(): void {
    this.isGroupSettingsModalOpen.set(false);
  }

  handleGroupChatUpdated(updatedChat: Chat): void {
    this.applyUpdatedChat(updatedChat);
    this.reloadCurrentMessages();
  }

  isOwnMessage(message: Message): boolean {
    return message.sender.userId === this.currentUser()?.id;
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
    const messageAlreadyExist = this.messages().some(
      (message) => message.id === normalizedMessage.id,
    );

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

  sendMessage(content: string): void {
    const chatId = this.selectedChatId();
    const replyToMessageId = this.replyingTo()?.id;

    if (!chatId || !content) {
      return;
    }

    this.stompService.publishJson('/app/chat.send', {
      chatId: chatId,
      content: content,
      replyToMessageId,
    });

    this.cancelReply();
  }

  async toggleAudioRecording(): Promise<void> {
    if (this.isRecordingAudio()) {
      this.stopAudioRecording();
      return;
    }

    await this.startAudioRecording();
  }

  private async startAudioRecording(): Promise<void> {
    if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === 'undefined') {
      this.audioRecordingError.set('Audio recording is not supported in this browser.');
      return;
    }

    try {
      this.audioRecordingError.set(null);
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mimeType = this.getPreferredAudioMimeType();
      const recorder = mimeType
        ? new MediaRecorder(stream, { mimeType })
        : new MediaRecorder(stream);

      this.recordingStream = stream;
      this.mediaRecorder = recorder;
      this.audioChunks = [];
      this.recordingStartedAt = Date.now();

      recorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          this.audioChunks.push(event.data);
        }
      };

      recorder.onerror = () => {
        this.audioRecordingError.set('Could not record audio.');
        this.stopRecordingStream();
        this.isRecordingAudio.set(false);
      };

      recorder.onstop = () => {
        const durationMs = Math.max(Date.now() - this.recordingStartedAt, 0);
        const audioType = recorder.mimeType || mimeType || 'audio/webm';
        const blob = new Blob(this.audioChunks, { type: audioType });

        this.stopRecordingStream();
        this.isRecordingAudio.set(false);
        this.audioChunks = [];

        if (blob.size === 0) {
          this.audioRecordingError.set('Recorded audio was empty.');
          return;
        }

        this.uploadVoiceMessage(blob, durationMs);
      };

      recorder.start();
      this.isRecordingAudio.set(true);
    } catch (err) {
      console.error('Could not start audio recording', err);
      this.audioRecordingError.set('Could not access the microphone.');
      this.stopRecordingStream();
      this.isRecordingAudio.set(false);
    }
  }

  private stopAudioRecording(): void {
    if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
      this.mediaRecorder.stop();
      return;
    }

    this.stopRecordingStream();
    this.isRecordingAudio.set(false);
  }

  private uploadVoiceMessage(blob: Blob, durationMs: number): void {
    const chatId = this.selectedChatId();
    const replyToMessageId = this.replyingTo()?.id;

    if (!chatId) {
      return;
    }

    this.isUploadingVoiceMessage.set(true);
    this.audioRecordingError.set(null);

    this.chatService
      .sendVoiceMessage(chatId, blob, durationMs, replyToMessageId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (message) => {
          const normalizedMessage = this.normalizeMessage(message);
          const messageAlreadyExists = this.messages().some(
            (candidate) => candidate.id === normalizedMessage.id,
          );

          if (!messageAlreadyExists) {
            this.messages.update((messages) => [...messages, normalizedMessage]);
          }

          this.cancelReply();
          this.isUploadingVoiceMessage.set(false);
        },
        error: (err) => {
          console.error('Could not upload voice message', err);
          this.audioRecordingError.set(this.getErrorMessage(err, 'Could not send voice message.'));
          this.isUploadingVoiceMessage.set(false);
        },
      });
  }

  private getPreferredAudioMimeType(): string {
    const supportedTypes = [
      'audio/webm;codecs=opus',
      'audio/webm',
      'audio/ogg;codecs=opus',
      'audio/ogg',
      'audio/mp4',
    ];

    return supportedTypes.find((type) => MediaRecorder.isTypeSupported(type)) ?? '';
  }

  private stopRecordingStream(): void {
    this.recordingStream?.getTracks().forEach((track) => track.stop());
    this.recordingStream = null;
    this.mediaRecorder = null;
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

  isVoiceMessage(message: Message): boolean {
    return message.type === 'VOICE';
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
    const normalizedMessage = {
      ...message,
      type: message.type ?? 'TEXT',
      reactions: message.reactions ?? [],
      starredByMe: message.starredByMe ?? false,
      systemMessage: message.systemMessage ?? false,
    };

    this.ensureAudioObjectUrl(normalizedMessage);

    return normalizedMessage;
  }

  private ensureAudioObjectUrl(message: Message): void {
    if (!this.isVoiceMessage(message) || !message.audioId) {
      return;
    }

    if (this.audioObjectUrls()[message.audioId] || this.loadingAudioIds.has(message.audioId)) {
      return;
    }

    const audioId = message.audioId;
    this.loadingAudioIds.add(audioId);

    this.chatService
      .getAudioBlob(audioId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (blob) => {
          const objectUrl = URL.createObjectURL(blob);
          this.audioObjectUrls.update((urls) => {
            const existingUrl = urls[audioId];
            if (existingUrl) {
              URL.revokeObjectURL(existingUrl);
            }

            return {
              ...urls,
              [audioId]: objectUrl,
            };
          });
          this.loadingAudioIds.delete(audioId);
        },
        error: (err) => {
          console.error('Could not load audio message', err);
          this.loadingAudioIds.delete(audioId);
        },
      });
  }
}
