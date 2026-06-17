import { Component, inject } from '@angular/core';
import { AuthService } from '@core/services/auth.service';
import { UserService } from '@core/services/user.service';
import { SidebarComponent } from '@shared/components/sidebar/sidebar.component';

@Component({
  templateUrl: './home.component.html',
  imports: [SidebarComponent],
})
export class HomeComponent {
  // readonly userService = inject(UserService);
  // readonly authService = inject(AuthService);

  // private readonly destroyRef = inject(DestroyRef);
  // chatModel = signal({
  //   content: '',
  // });
  // chatForm = form(this.chatModel, {
  //   submission: {
  //     action: async (field) => {
  //       this.sendMessage(field().value().content);
  //     },
  //   },
  // });
  //
  // chatMessage = signal('');
  //
  // private readonly stompService = inject(StompService);
  // private chatSubscription: StompSubscription | null = null;

  async ngOnInit() {
    // this.stompService.connect(() => {
    //   this.chatSubscription = this.stompService.subscribe('/topic/messages', (message) => {
    //     this.chatMessage.set(message.body);
    //   });
    //
    //   this.destroyRef.onDestroy(() => {
    //     this.chatSubscription?.unsubscribe();
    //   });
    // });
  }

  //
  // sendMessage(content: string) {
  //   this.stompService.publish('/app/chat', content);
  // }
}
