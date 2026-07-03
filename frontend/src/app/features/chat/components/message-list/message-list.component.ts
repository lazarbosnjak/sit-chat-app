import { formatDate } from '@angular/common';
import {
  afterNextRender,
  Component,
  effect,
  ElementRef,
  inject,
  Injector,
  input,
  output,
  ViewChild,
} from '@angular/core';
import {
  Message,
  MessageDeliveryStatus,
  MessageReactionType,
  MessageReference,
} from '@shared/types/api.types';
import { LucideForward, LucideReply, LucideStar } from '@lucide/angular';
import { MessageReactionRequest, ReactionOption } from '../../chat.types';

@Component({
  selector: 'app-message-list',
  templateUrl: './message-list.component.html',
  host: {
    class: 'min-h-0 flex-1',
  },
  imports: [LucideForward, LucideReply, LucideStar],
})
export class MessageListComponent {
  messages = input.required<Message[]>();
  currentUserId = input.required<string | null>();
  reactionOptions = input.required<ReactionOption[]>();
  audioObjectUrls = input.required<Record<string, string>>();

  openUserProfile = output<string>();
  startReply = output<Message>();
  openForward = output<Message>();
  toggleStarred = output<Message>();
  react = output<MessageReactionRequest>();

  @ViewChild('messagesContainer')
  private messagesContainer?: ElementRef<HTMLElement>;
  private readonly injector = inject(Injector);

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

  isOwnMessage(message: Message): boolean {
    return message.sender.userId === this.currentUserId();
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

  getReplyReference(message: Message): MessageReference | null {
    return message.replyTo ?? this.findMessageReference(message.replyToMessageId);
  }

  getForwardedReference(message: Message): MessageReference | null {
    return message.forwardedFrom ?? this.findMessageReference(message.forwardedFromMessageId);
  }

  shouldRenderContent(message: Message): boolean {
    if (this.isVoiceMessage(message)) {
      return false;
    }

    const forwardedReference = this.getForwardedReference(message);

    return !forwardedReference || forwardedReference.content !== message.content;
  }

  isVoiceMessage(message: Message): boolean {
    return message.type === 'VOICE';
  }

  getAudioUrl(message: Message): string | null {
    if (!message.audioId) {
      return null;
    }

    return this.audioObjectUrls()[message.audioId] ?? null;
  }

  formatAudioDuration(durationMs: number | null | undefined): string {
    if (!durationMs || durationMs < 0) {
      return '0:00';
    }

    const totalSeconds = Math.round(durationMs / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;

    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }

  emojiForReaction(type: MessageReactionType): string {
    return this.reactionOptions().find((reaction) => reaction.type === type)?.emoji ?? '';
  }

  labelForReaction(type: MessageReactionType): string {
    return this.reactionOptions().find((reaction) => reaction.type === type)?.label ?? type;
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

  private scrollToBottom() {
    const el = this.messagesContainer?.nativeElement;

    if (!el) {
      return;
    }

    el.scrollTop = el.scrollHeight;
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

  private isSameDay(a: Date, b: Date): boolean {
    return (
      a.getFullYear() === b.getFullYear() &&
      a.getMonth() === b.getMonth() &&
      a.getDate() === b.getDate()
    );
  }
}
