import { Routes } from '@angular/router';
import { AnalyticsComponent } from '@features/admin/analytics/analytics.component';
import { AdminDashboardComponent } from '@features/admin/dashboard/dashboard.component';
import { RegistrationRequestsComponent } from '@features/admin/registration-requests/registration-request.component';
import { AdminUsersComponent } from '@features/admin/users/admin.users.component';

export const ADMIN_ROUTES: Routes = [
  {
    path: 'dashboard',
    component: AdminDashboardComponent,
  },
  {
    path: 'users',
    component: AdminUsersComponent,
  },
  {
    path: 'analytics',
    component: AnalyticsComponent,
  },
  {
    path: 'registration-requests',
    component: RegistrationRequestsComponent,
  },
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
];
