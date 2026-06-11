import { Routes } from '@angular/router';
import { AdminDashboardComponent } from '@features/admin/dashboard/dashboard.component';
import { RegistrationRequestsComponent } from '@features/admin/registration-requests/registration-request.component';

export const ADMIN_ROUTES: Routes = [
  {
    path: 'dashboard',
    component: AdminDashboardComponent,
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
