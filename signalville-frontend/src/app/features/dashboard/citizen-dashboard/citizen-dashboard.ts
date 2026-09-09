import { Component, OnInit, computed, inject, signal } from '@angular/core';

import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../core/auth/auth';
import { Dashboards } from '../../../core/services/dashboard';
import { CitizenDashboardResponse } from '../../../shared/models/api.models';

@Component({
  selector: 'app-citizen-dashboard',
  imports: [RouterLink, DatePipe],
  templateUrl: './citizen-dashboard.html',
  styleUrl: './citizen-dashboard.scss',
})
export class CitizenDashboard implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly dashboardsService = inject(Dashboards);

  readonly currentUser = this.authService.currentUser;

  readonly dashboard = signal<CitizenDashboardResponse | null>(null);
  readonly isLoading = signal(true);
  readonly loadError = signal<string | null>(null);

  readonly totalCount = computed(() => this.dashboard()?.totalReports ?? 0);
  readonly newCount = computed(() => this.dashboard()?.byStatus['NOUVEAU'] ?? 0);
  readonly inProgressCount = computed(() => this.dashboard()?.byStatus['EN_COURS'] ?? 0);
  readonly resolvedCount = computed(() => this.dashboard()?.resolvedReports ?? 0);

  readonly recentReports = computed(() => (this.dashboard()?.recentReports ?? []).slice(0, 5));

  ngOnInit(): void {
    this.dashboardsService.getCitizenDashboard().subscribe({
      next: dashboard => {
        this.dashboard.set(dashboard);
        this.isLoading.set(false);
      },
      error: () => {
        this.loadError.set('Impossible de charger vos signalements.');
        this.isLoading.set(false);
      },
    });
  }
}