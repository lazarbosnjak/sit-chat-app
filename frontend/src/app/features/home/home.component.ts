import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { form, FormField, FormRoot } from '@angular/forms/signals';
import { StompService } from '@core/services/stomp.service';
import { environment as env } from '@environments/environment';
import { StompSubscription } from '@stomp/stompjs';

interface User {
  username: string;
}

@Component({
  templateUrl: './home.component.html',
  imports: [FormField, FormRoot],
})
export class HomeComponent {
  private readonly destroyRef = inject(DestroyRef);
  private http = inject(HttpClient);
  username = signal('');

  chatModel = signal({
    content: '',
  });
  chatForm = form(this.chatModel, {
    submission: {
      action: async (field) => {
        this.sendMessage(field().value().content);
      },
    },
  });

  chatMessage = signal('');

  private readonly stompService = inject(StompService);
  private chatSubscription: StompSubscription | null = null;

  ngOnInit() {
    this.http.get<User>(`${env.apiUrl}/users/me`).subscribe({
      next: (user) => {
        console.log(user.username);
        this.username.set(user.username);
      },
      error: (err: Error) => {
        console.error(err);
      },
    });

    this.stompService.connect(() => {
      this.chatSubscription = this.stompService.subscribe('/topic/messages', (message) => {
        this.chatMessage.set(message.body);
      });

      this.destroyRef.onDestroy(() => {
        this.chatSubscription?.unsubscribe();
      });
    });
  }

  sendMessage(content: string) {
    this.stompService.publish('/app/chat', content);
  }
}
