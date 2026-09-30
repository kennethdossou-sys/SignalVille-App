import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Loader } from '../../../shared/components/loader/loader';

import { Interventions } from '../../../core/services/interventions';
import { Dashboards } from '../../../core/services/dashboard';
import {
  AvailableAgentResponse,
  ReportResponse,
  SupervisorDashboardResponse,
} from '../../../shared/models/api.models';

@Component({
  selector: 'app-supervisor-dashboard',
  standalone: true,
  imports: [FormsModule, RouterLink, Loader],
  templateUrl: './supervisor-dashboard.html',
  styleUrl: './supervisor-dashboard.scss',
})
export class SupervisorDashboard implements OnInit {
  private readonly interventionsService = inject(Interventions);
  private readonly dashboardsService = inject(Dashboards);

  // === Etat ===
  readonly dashboard = signal<SupervisorDashboardResponse | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly actionInProgress = signal<string | null>(null);

  // Listes derivees du dashboard, pour garder les templates existants
  // inchanges autant que possible.
  readonly newReports = computed<ReportResponse[]>(() => this.dashboard()?.unassignedReports ?? []);
  readonly resolvedReports = computed<ReportResponse[]>(() => this.dashboard()?.reportsToVerify ?? []);
  readonly criticalReports = computed<ReportResponse[]>(() => this.dashboard()?.criticalReports ?? []);
  readonly reopenedReports = computed<ReportResponse[]>(() => this.dashboard()?.reopenedReports ?? []);

  readonly availableAgents = signal<AvailableAgentResponse[]>([]);

  // Formulaire d'affectation : quel signalement est en cours d'affectation,
  // et l'agent + instruction saisis pour lui.
  readonly assigningReportId = signal<string | null>(null);
  selectedAgentId = '';
  assignInstruction = '';

  // Formulaire de rejet.
  readonly rejectingReportId = signal<string | null>(null);
  rejectReason = '';

  // Formulaire de reouverture.
  readonly reopeningReportId = signal<string | null>(null);
  reopenReason = '';

  readonly hasNewReports = computed(() => this.newReports().length > 0);
  readonly hasResolvedReports = computed(() => this.resolvedReports().length > 0);

  ngOnInit(): void {
    this.loadAll();
  }

  private loadAll(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.dashboardsService.getSupervisorDashboard().subscribe({
      next: dashboard => {
        this.dashboard.set(dashboard);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger le tableau de bord.');
        this.loading.set(false);
      },
    });

    // Quand on ouvre le formulaire d'affectation pour un signalement REOUVERT,
    // reportId est transmis a listAvailableAgents pour exclure l'agent
    // precedent (regle metier Seance 4). Charge sans filtre au demarrage ;
    // recharge cible depuis openAssignForm().
    this.interventionsService.listAvailableAgents().subscribe({
      next: agents => this.availableAgents.set(agents),
      error: () => this.errorMessage.set('Impossible de charger la liste des agents.'),
    });
  }

  // === Affectation ===

  openAssignForm(reportId: string): void {
    this.assigningReportId.set(reportId);
    this.selectedAgentId = '';
    this.assignInstruction = '';
  }

  cancelAssignForm(): void {
    this.assigningReportId.set(null);
  }

  confirmAssign(reportId: string): void {
    if (!this.selectedAgentId) {
      this.errorMessage.set('Choisissez un agent avant de confirmer.');
      return;
    }
    this.actionInProgress.set(reportId);
    this.interventionsService
      .assign(reportId, { agentId: this.selectedAgentId, instruction: this.assignInstruction || undefined })
      .subscribe({
        next: () => {
          this.assigningReportId.set(null);
          this.actionInProgress.set(null);
          this.loadAll();
        },
        error: () => {
          this.errorMessage.set("Echec de l'affectation. Verifiez que le signalement est encore au statut NOUVEAU ou REOUVERT.");
          this.actionInProgress.set(null);
        },
      });
  }

  // === Rejet ===

  openRejectForm(reportId: string): void {
    this.rejectingReportId.set(reportId);
    this.rejectReason = '';
  }

  cancelRejectForm(): void {
    this.rejectingReportId.set(null);
  }

  confirmReject(reportId: string): void {
    if (!this.rejectReason.trim()) {
      this.errorMessage.set('Un motif de rejet est obligatoire.');
      return;
    }
    this.actionInProgress.set(reportId);
    this.interventionsService.reject(reportId, this.rejectReason).subscribe({
      next: () => {
        this.rejectingReportId.set(null);
        this.actionInProgress.set(null);
        this.loadAll();
      },
      error: () => {
        this.errorMessage.set('Echec du rejet.');
        this.actionInProgress.set(null);
      },
    });
  }

  // === Cloture ===

  confirmClose(reportId: string): void {
    this.actionInProgress.set(reportId);
    this.interventionsService.close(reportId).subscribe({
      next: () => {
        this.actionInProgress.set(null);
        this.loadAll();
      },
      error: () => {
        this.errorMessage.set('Echec de la cloture. Verifiez que le signalement est bien RESOLU.');
        this.actionInProgress.set(null);
      },
    });
  }

  // === Reouverture ===

  openReopenForm(reportId: string): void {
    this.reopeningReportId.set(reportId);
    this.reopenReason = '';
  }

  cancelReopenForm(): void {
    this.reopeningReportId.set(null);
  }

  confirmReopen(reportId: string): void {
    if (!this.reopenReason.trim()) {
      this.errorMessage.set('Un motif de reouverture est obligatoire.');
      return;
    }
    this.actionInProgress.set(reportId);
    this.interventionsService.reopen(reportId, this.reopenReason).subscribe({
      next: () => {
        this.reopeningReportId.set(null);
        this.actionInProgress.set(null);
        this.loadAll();
      },
      error: () => {
        this.errorMessage.set('Echec de la reouverture.');
        this.actionInProgress.set(null);
      },
    });
  }
}