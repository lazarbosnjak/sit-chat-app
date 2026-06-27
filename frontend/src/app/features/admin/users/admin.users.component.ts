import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import {
  debounce,
  disabled,
  email,
  form,
  FormField,
  FormRoot,
  maxLength,
  required,
} from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { environment as env } from '@environments/environment';
import {
  DataTableComponent,
  TableColumn,
} from '@shared/components/data-table/data-table.component';
import { ModalComponent } from '@shared/components/modal/modal.component';
import { User } from '@shared/types/api.types';
import { firstValueFrom } from 'rxjs';

interface UpdateUserModel {
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  pfpUrl: string;
  enabled: boolean;
  role: 'ADMIN' | 'USER';
}

@Component({
  templateUrl: './admin.users.component.html',
  imports: [DataTableComponent, ModalComponent, FormField, FormRoot, DatePipe, RouterLink],
})
export class AdminUsersComponent {
  private readonly http = inject(HttpClient);

  columns: TableColumn<User>[] = [
    { key: 'username', label: 'Username' },
    { key: 'firstName', label: 'First name' },
    { key: 'lastName', label: 'Last name' },
    { key: 'email', label: 'E-mail' },
    { key: 'phoneNumber', label: 'Phone number' },
    { key: 'role', label: 'Role' },
    { key: 'enabled', label: 'Status' },
  ];

  users = signal<User[]>([]);
  selectedUser = signal<User | null>(null);
  updateUserEnabled = signal(false);

  updateUserModel = signal<UpdateUserModel>({
    firstName: '',
    lastName: '',
    email: '',
    phoneNumber: '',
    pfpUrl: '',
    enabled: false,
    role: 'USER',
  });

  updateUserForm = form(
    this.updateUserModel,
    (schemaPath) => {
      debounce(schemaPath, 'blur');
      disabled(schemaPath, { when: () => !this.updateUserEnabled() });

      required(schemaPath.firstName, { message: 'First name is required' });
      required(schemaPath.lastName, { message: 'Last name is required' });
      required(schemaPath.email, { message: 'E-mail is required' });
      required(schemaPath.phoneNumber, { message: 'Phone number is required' });

      email(schemaPath.email, { message: 'Enter a valid e-mail address' });

      maxLength(schemaPath.phoneNumber, 15, {
        message: 'Phone number cant be longer than 15 characters',
      });
    },
    {
      submission: {
        action: async (field) => {
          const currentUser = this.selectedUser();
          if (!currentUser) {
            throw new Error('User does not exist');
          }

          const userToUpdate: User = {
            ...currentUser,
            ...field().value(),
          };

          try {
            const updatedUser = await firstValueFrom(
              this.http.put<User>(`${env.apiUrl}/admin/users/${currentUser.id}`, userToUpdate),
            );

            this.selectedUser.set(updatedUser);
            this.users.update((users) =>
              users.map((user) => (user.id === updatedUser.id ? updatedUser : user)),
            );
            this.updateUserEnabled.set(false);
          } catch (error) {
            console.error(error);
          }
        },
        onInvalid: (field) => {
          const firstError = field().errorSummary()[0];
          firstError.fieldTree().focusBoundControl();
        },
      },
    },
  );

  ngOnInit() {
    this.http.get<User[]>(`${env.apiUrl}/admin/users`).subscribe({
      next: (res) => {
        this.users.set(res);
      },
      error: (err) => {
        console.error(err);
      },
    });
  }

  openUserModal(user: User) {
    this.selectedUser.set(user);
    this.setupUpdateForm(user);
    this.updateUserEnabled.set(false);
  }

  closeUserModal() {
    this.selectedUser.set(null);
    this.updateUserEnabled.set(false);
  }

  setupUpdateForm(user: User) {
    this.updateUserModel.set({
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      phoneNumber: user.phoneNumber,
      pfpUrl: user.pfpUrl,
      enabled: user.enabled,
      role: user.role,
    });
  }

  cancelUpdate() {
    const user = this.selectedUser();
    if (!user) return;

    this.setupUpdateForm(user);
    this.updateUserEnabled.set(false);
  }

  showError(field: { touched: () => boolean; dirty: () => boolean; invalid: () => boolean }) {
    return this.updateUserEnabled() && field.invalid() && (field.touched() || field.dirty());
  }

  firstError(field: { errors: () => readonly { message?: string; kind: string }[] }) {
    return field.errors()[0]?.message ?? 'Invalid value';
  }
}
