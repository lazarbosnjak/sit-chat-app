import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  templateUrl: './dashboard.component.html',
})
export class AdminDashboardComponent {
  private readonly router = inject(Router);

  navigateTo(location: string) {
    this.router.navigate([location]);
  }
}
