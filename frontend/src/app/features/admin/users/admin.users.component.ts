import { DatePipe } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
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
  blockType: 'TEMPORARY' | 'PERMANENT';
  blockReason: string;
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
  blockingMenuOpen = signal(false);
  updateError = signal<string | null>(null);

  updateUserModel = signal<UpdateUserModel>({
    firstName: '',
    lastName: '',
    email: '',
    phoneNumber: '',
    pfpUrl: '',
    enabled: false,
    role: 'USER',
    blockType: 'TEMPORARY',
    blockReason: '',
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

          this.updateError.set(null);
          const updateValue = field().value();

          if (!updateValue.enabled && !updateValue.blockReason.trim()) {
            this.updateError.set('Block reason is required');
            return;
          }

          await this.saveUser(updateValue);
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
    this.blockingMenuOpen.set(false);
    this.updateError.set(null);
  }

  closeUserModal() {
    this.selectedUser.set(null);
    this.updateUserEnabled.set(false);
    this.blockingMenuOpen.set(false);
    this.updateError.set(null);
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
      blockType: user.blockType ?? 'TEMPORARY',
      blockReason: user.blockReason ?? '',
    });
  }

  cancelUpdate() {
    const user = this.selectedUser();
    if (!user) return;

    this.setupUpdateForm(user);
    this.updateUserEnabled.set(false);
    this.blockingMenuOpen.set(false);
    this.updateError.set(null);
  }

  openBlockingMenu() {
    this.updateUserEnabled.set(true);
    this.blockingMenuOpen.set(true);
    this.updateError.set(null);
    this.updateUserModel.update((user) => ({
      ...user,
      enabled: false,
      blockType: user.blockType ?? 'TEMPORARY',
      blockReason: '',
    }));
  }

  async unblockUser() {
    this.updateError.set(null);
    await this.saveUser({
      ...this.updateUserModel(),
      enabled: true,
      blockReason: '',
    });
  }

  private async saveUser(updateValue: UpdateUserModel) {
    const currentUser = this.selectedUser();
    if (!currentUser) {
      throw new Error('User does not exist');
    }

    const userToUpdate: User = {
      ...currentUser,
      ...updateValue,
      blockReason: updateValue.blockReason.trim(),
    };

    try {
      const updatedUser = await firstValueFrom(
        this.http.put<User>(`${env.apiUrl}/admin/users/${currentUser.id}`, userToUpdate),
      );

      this.selectedUser.set(updatedUser);
      this.users.update((users) =>
        users.map((user) => (user.id === updatedUser.id ? updatedUser : user)),
      );
      this.setupUpdateForm(updatedUser);
      this.updateUserEnabled.set(false);
      this.blockingMenuOpen.set(false);
    } catch (error) {
      if (error instanceof HttpErrorResponse) {
        this.updateError.set(error.error?.detail ?? 'Unable to update user');
        return;
      }
      console.error(error);
    }
  }

  showError(field: { touched: () => boolean; dirty: () => boolean; invalid: () => boolean }) {
    return this.updateUserEnabled() && field.invalid() && (field.touched() || field.dirty());
  }

  firstError(field: { errors: () => readonly { message?: string; kind: string }[] }) {
    return field.errors()[0]?.message ?? 'Invalid value';
  }
}
