import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  DataTableComponent,
  TableColumn,
} from '@shared/components/data-table/data-table.component';
import { ModalComponent } from '@shared/components/modal/modal.component';
import { UserService } from '@core/services/user.service';
import { User } from '@shared/types/api.types';

@Component({
  templateUrl: './admin.users.component.html',
  imports: [DataTableComponent, ModalComponent, FormsModule],
})
export class AdminUsersComponent {
  private readonly userService = inject(UserService);

  users = signal<User[]>([]);
  selectedUser = signal<User | null>(null);

  columns: TableColumn<User>[] = [
    { key: 'username', label: 'Username' },
    { key: 'firstName', label: 'First name' },
    { key: 'lastName', label: 'Last name' },
    { key: 'email', label: 'Email' },
    { key: 'phoneNumber', label: 'Phone number' },
    { key: 'role', label: 'Role' },
    { key: 'enabled', label: 'Enabled' },
  ];

  ngOnInit() {
    this.userService.getAllUsers().subscribe({
      next: (users) => this.users.set(users),
      error: (err) => console.log(err),
    });
  }

  openUserModal(user: User) {
    this.selectedUser.set({ ...user });
  }

  closeUserModal() {
    this.selectedUser.set(null);
  }

  async saveUser() {
    const user = this.selectedUser();
    if (!user) return;

    const savedUser = await this.userService.updateUser(user);

    this.users.update((users) => users.map((u) => (u.id === savedUser.id ? savedUser : u)));

    this.closeUserModal();
  }
}
