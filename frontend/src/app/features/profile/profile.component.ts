import { DatePipe } from '@angular/common';
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
import { UserService } from '@core/services/user.service';
import { User } from '@shared/types/api.types';

interface UpdateProfileModel {
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  pfpUrl: string;
  status: string;
  aboutMe: string;
}

@Component({
  templateUrl: './profile.component.html',
  imports: [FormField, FormRoot, DatePipe, RouterLink],
})
export class ProfileComponent {
  private readonly userService = inject(UserService);

  user = signal<User | null>(null);

  updateProfileEnabled = signal<boolean>(false);
  updateProfileModel = signal<UpdateProfileModel>({
    firstName: '',
    lastName: '',
    email: '',
    phoneNumber: '',
    pfpUrl: '',
    status: '',
    aboutMe: '',
  });

  updateProfileForm = form(
    this.updateProfileModel,
    (schemaPath) => {
      debounce(schemaPath, 'blur');
      disabled(schemaPath, { when: () => !this.updateProfileEnabled() });

      required(schemaPath.firstName, { message: 'First name is required' });
      required(schemaPath.lastName, { message: 'Last name is required' });
      required(schemaPath.email, { message: 'E-mail is required' });
      required(schemaPath.phoneNumber, { message: 'Phone number is required' });

      email(schemaPath.email, { message: 'Enter a valid e-mail address' });

      maxLength(schemaPath.phoneNumber, 15, {
        message: 'Phone number cant be longer than 15 characters',
      });
      maxLength(schemaPath.status, 80, {
        message: 'Status cant be longer than 80 characters',
      });
      maxLength(schemaPath.aboutMe, 500, {
        message: 'About me cant be longer than 500 characters',
      });
    },
    {
      submission: {
        action: async (field) => {
          const currentUser = this.user();
          if (!currentUser) {
            throw new Error('User does not exist');
          }
          const userToUpdate: User = {
            ...currentUser,
            ...field().value(),
          };
          try {
            const updatedUser = await this.userService.updateUser(userToUpdate);

            this.updateProfileEnabled.set(false);
            this.user.set(updatedUser);
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
    const user = this.userService.getLoggedInUser();
    if (user) {
      this.user.set(user);

      this.setupUpdateForm(user);
    }
  }

  setupUpdateForm(user: User) {
    this.updateProfileModel.set({
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      phoneNumber: user.phoneNumber,
      pfpUrl: user.pfpUrl,
      status: user.status ?? '',
      aboutMe: user.aboutMe ?? '',
    });
  }

  cancelUpdate() {
    const user = this.user();
    if (!user) return;

    this.setupUpdateForm(user);
    this.updateProfileEnabled.set(false);
  }

  showError(field: { touched: () => boolean; dirty: () => boolean; invalid: () => boolean }) {
    return this.updateProfileEnabled() && field.invalid() && (field.touched() || field.dirty());
  }

  firstError(field: { errors: () => readonly { message?: string; kind: string }[] }) {
    return field.errors()[0]?.message ?? 'Invalid value';
  }
}
