import { inject } from '@angular/core';
import { CanActivateChildFn, Router } from '@angular/router';
import { ROLE_ADMIN } from '@core/constants/auth.constants';
import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateChildFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.hasRole(ROLE_ADMIN)) {
    return true;
  }

  return router.createUrlTree(['/']);
};
