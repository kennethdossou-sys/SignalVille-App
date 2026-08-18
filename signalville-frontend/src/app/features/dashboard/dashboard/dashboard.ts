import { Component, OnInit, computed, inject, signal } from '@angular/core';

import { DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/auth/auth';
import { Reports } from '../../reports/reports';
import { ReportResponse } from '../../../shared/models/api.models';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, DatePipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly reportsService = inject(Reports);
  private readonly router = inject(Router);

  readonly currentUser = this.authService.currentUser;

  readonly reports = signal<ReportResponse[]>([]);
  readonly isLoading = signal(true);
  readonly loadError = signal<string | null>(null);

  readonly totalCount = computed(() => this.reports().length);
  readonly newCount = computed(
    () => this.reports().filter(r => r.status === 'NOUVEAU').length
  );
  readonly inProgressCount = computed(
    () => this.reports().filter(r => r.status === 'EN_COURS').length
  );
  readonly resolvedCount = computed(
    () => this.reports().filter(r => r.status === 'RESOLU').length
  );

  readonly recentReports = computed(() => this.reports().slice(0, 5));

  ngOnInit(): void {
  this.reportsService.list({ page: 0, size: 50 }).subscribe({
      next: (page) => {
        this.reports.set(page.content);
        this.isLoading.set(false);
      },
      error: () => {
        this.loadError.set('Impossible de charger vos signalements.');
        this.isLoading.set(false);
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