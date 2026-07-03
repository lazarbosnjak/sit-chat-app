import { Component, input, output, signal } from '@angular/core';
import { disabled, form, FormField, FormRoot, required } from '@angular/forms/signals';
import { Message } from '@shared/types/api.types';
import { LucideMic, LucideSend, LucideSquare, LucideX } from '@lucide/angular';

@Component({
  selector: 'app-message-composer',
  templateUrl: './message-composer.component.html',
  imports: [FormField, FormRoot, LucideMic, LucideSend, LucideSquare, LucideX],
})
export class MessageComposerComponent {
  replyingTo = input.required<Message | null>();
  isRecordingAudio = input(false);
  isUploadingVoiceMessage = input(false);
  audioRecordingError = input<string | null>(null);

  sendText = output<string>();
  toggleAudioRecording = output<void>();
  cancelReply = output<void>();

  messageModel = signal({
    content: '',
  });

  messageForm = form(this.messageModel, (path) => {
    disabled(path.content, {
      when: () => this.isRecordingAudio() || this.isUploadingVoiceMessage(),
    });

    required(path.content, {
      message: 'Message cannot be empty',
    });
  });

  sendMessage(): void {
    const content = this.messageModel().content.trim();

    if (!content || this.isRecordingAudio() || this.isUploadingVoiceMessage()) {
      return;
    }

    this.sendText.emit(content);
    this.messageModel.set({
      content: '',
    });
  }
}
