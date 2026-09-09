import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/auth/auth';
import { Dashboards } from '../../../core/services/dashboard';
import { AdminDashboardResponse } from '../../../shared/models/api.models';
import { KeyValuePipe } from '@angular/common';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [RouterLink,KeyValuePipe],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.scss',
})
export class AdminDashboard implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly dashboardsService = inject(Dashboards);
  private readonly router = inject(Router);

  readonly currentUser = this.authService.currentUser;

  readonly dashboard = signal<AdminDashboardResponse | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.dashboardsService.getAdminDashboard().subscribe({
      next: dashboard => {
        this.dashboard.set(dashboard);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger le tableau de bord.');
        this.loading.set(false);
      },
    });
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => {
        this.authService.forceLocalLogout();
        this.router.navigate(['/login']);
      },
    });
  }
}