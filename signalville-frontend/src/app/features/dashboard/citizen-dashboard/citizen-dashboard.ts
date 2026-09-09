import { Component, OnInit, computed, inject, signal } from '@angular/core';

import { DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';

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
  private readonly router = inject(Router);

  readonly currentUser = this.authService.currentUser;

  readonly dashboard = signal<CitizenDashboardResponse | null>(null);
  readonly isLoading = signal(true);
  readonly loadError = signal<string | null>(null);

  // Compteurs derives, calcules une seule fois cote serveur (source de
  // verite unique) plutot que recalcules ici a partir d'une liste partielle
  // — corrige une incoherence de l'ancienne version, ou resolvedCount ne
  // comptait que RESOLU, jamais CLOTURE (decision Seance 4 : les deux sont
  // "regle" du point de vue du citoyen).
  readonly totalCount = computed(() => this.dashboard()?.totalReports ?? 0);
  readonly newCount = computed(() => this.dashboard()?.byStatus['NOUVEAU'] ?? 0);
  readonly inProgressCount = computed(() => this.dashboard()?.byStatus['EN_COURS'] ?? 0);
  readonly resolvedCount = computed(() => this.dashboard()?.resolvedReports ?? 0);

  // Le backend renvoie deja une liste courte (max 10) ; on affiche les 5
  // premieres pour garder la densite visuelle d'origine de cet ecran.
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