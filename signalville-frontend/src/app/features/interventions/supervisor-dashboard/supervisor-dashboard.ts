import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Reports } from '../../reports/reports';
import { Interventions } from '../interventions';
import {
  AvailableAgentResponse,
  ReportResponse,
} from '../../../shared/models/api.models';

@Component({
  selector: 'app-supervisor-dashboard',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './supervisor-dashboard.html',
  styleUrl: './supervisor-dashboard.scss',
})
export class SupervisorDashboard implements OnInit {
  private readonly reportsService = inject(Reports);
  private readonly interventionsService = inject(Interventions);

  // === Etat ===
  readonly newReports = signal<ReportResponse[]>([]);
  readonly resolvedReports = signal<ReportResponse[]>([]);
  readonly availableAgents = signal<AvailableAgentResponse[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly actionInProgress = signal<string | null>(null);

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

    this.reportsService.list({ status: 'NOUVEAU', size: 50 }).subscribe({
      next: page => this.newReports.set(page.content),
      error: () => this.errorMessage.set('Impossible de charger les nouveaux signalements.'),
    });

    this.reportsService.list({ status: 'RESOLU', size: 50 }).subscribe({
      next: page => this.resolvedReports.set(page.content),
      error: () => this.errorMessage.set('Impossible de charger les signalements resolus.'),
    });

    this.interventionsService.listAvailableAgents().subscribe({
      next: agents => {
        this.availableAgents.set(agents);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger la liste des agents.');
        this.loading.set(false);
      },
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
          this.errorMessage.set("Echec de l'affectation. Verifiez que le signalement est encore au statut NOUVEAU.");
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