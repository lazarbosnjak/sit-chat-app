import { Component, input, output } from '@angular/core';
import { ModalComponent } from '@shared/components/modal/modal.component';
import { User } from '@shared/types/api.types';

@Component({
  selector: 'app-user-profile-modal',
  templateUrl: './user-profile-modal.component.html',
  imports: [ModalComponent],
})
export class UserProfileModalComponent {
  user = input.required<User | null>();
  error = input<string | null>(null);

  close = output<void>();
}
