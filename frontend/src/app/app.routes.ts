import { Routes } from '@angular/router';
import { HomeComponent } from './features/home/home.component';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from '@core/guards/admin.guard';
import { ChatComponent } from '@features/chat/chat.component';
import { ProfileComponent } from '@features/profile/profile.component';
import { StarredMessagesComponent } from '@features/starred-messages/starred-messages.component';

export const routes: Routes = [
  {
    path: '',
    component: HomeComponent,
    canActivate: [authGuard],
  },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
    // TODO: add guard to prevent going to auth if logged in
  },
  {
    path: 'admin',
    loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
    canActivateChild: [adminGuard],
  },
  {
    path: 'chats/:id',
    component: ChatComponent,
    canActivate: [authGuard],
  },
  {
    path: 'profile',
    component: ProfileComponent,
    canActivate: [authGuard],
  },
  {
    path: 'starred-messages',
    component: StarredMessagesComponent,
    canActivate: [authGuard],
  },
];
