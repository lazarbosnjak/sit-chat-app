import { Component, inject, input, output } from '@angular/core';
import { Chat, Message } from '@shared/types/api.types';
import { ModalComponent } from '@shared/components/modal/modal.component';
import { ChatService } from '@core/services/chat.service';

@Component({
  selector: 'app-forward-message-modal',
  templateUrl: './forward-message-modal.component.html',
  imports: [ModalComponent],
})
export class ForwardMessageModalComponent {
  readonly chatService = inject(ChatService);

  message = input.required<Message | null>();
  chats = input.required<Chat[]>();

  close = output<void>();
  forward = output<Chat>();
}
