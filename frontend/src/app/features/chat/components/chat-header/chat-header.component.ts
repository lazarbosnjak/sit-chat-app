import { Component, input, output } from '@angular/core';
import { Chat, ChatMember } from '@shared/types/api.types';
import { LucideUsers } from '@lucide/angular';

@Component({
  selector: 'app-chat-header',
  templateUrl: './chat-header.component.html',
  imports: [LucideUsers],
})
export class ChatHeaderComponent {
  chat = input.required<Chat | null>();
  directRecipient = input.required<ChatMember | null>();
  title = input.required<string>();
  image = input.required<string>();

  openUserProfile = output<string>();
  openGroupSettings = output<void>();
}
