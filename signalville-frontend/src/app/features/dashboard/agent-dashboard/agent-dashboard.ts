import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';

import { Interventions } from '../../../core/services/interventions';
import { Dashboards } from '../../../core/services/dashboard';
import { AgentDashboardResponse, ReportResponse, NoteResponse } from '../../../shared/models/api.models';
import { AuthService } from '../../../core/auth/auth';
import { DecimalPipe } from '@angular/common';

@Component({
  selector: 'app-agent-dashboard',
  standalone: true,
  imports: [FormsModule, RouterLink,DecimalPipe],
  templateUrl: './agent-dashboard.html',
  styleUrl: './agent-dashboard.scss',
})
export class AgentDashboard implements OnInit {
  private readonly interventionsService = inject(Interventions);
  private readonly dashboardsService = inject(Dashboards);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  //Recuperer les infos de lutilisateur
  readonly currentUser = this.authService.currentUser;

  // Etat principal : un seul appel GET /dashboard/agent remplace desormais
  // les deux appels separes (AFFECTE + EN_COURS) fusionnes cote client.
  readonly dashboard = signal<AgentDashboardResponse | null>(null);
  readonly assignedReports = computed<ReportResponse[]>(() => this.dashboard()?.currentInterventions ?? []);

  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly actionInProgress = signal<string | null>(null);

  // Notes, chargees a la demande par signalement ouvert.
  readonly openNotesReportId = signal<string | null>(null);
  readonly notes = signal<NoteResponse[]>([]);
  newNoteContent = '';

  // Formulaire de resolution.
  readonly resolvingReportId = signal<string | null>(null);
  resolutionComment = '';
  selectedProofs: File[] = [];

  ngOnInit(): void {
    this.loadDashboard();
  }

  private loadDashboard(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.dashboardsService.getAgentDashboard().subscribe({
      next: dashboard => {
        this.dashboard.set(dashboard);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger vos interventions.');
        this.loading.set(false);
      },
    });
  }

  // === Demarrage ===

  startIntervention(reportId: string): void {
    this.actionInProgress.set(reportId);
    this.interventionsService.start(reportId).subscribe({
      next: () => {
        this.actionInProgress.set(null);
        this.loadDashboard();
      },
      error: () => {
        this.errorMessage.set("Echec du demarrage. Verifiez que l'intervention est bien au statut AFFECTEE.");
        this.actionInProgress.set(null);
      },
    });
  }

  // === Notes ===

  toggleNotes(reportId: string): void {
    if (this.openNotesReportId() === reportId) {
      this.openNotesReportId.set(null);
      return;
    }
    this.openNotesReportId.set(reportId);
    this.newNoteContent = '';
    this.interventionsService.listNotes(reportId).subscribe({
      next: notes => this.notes.set(notes),
      error: () => this.errorMessage.set('Impossible de charger les notes.'),
    });
  }

  addNote(reportId: string): void {
    if (!this.newNoteContent.trim()) {
      return;
    }
    this.interventionsService.addNote(reportId, { content: this.newNoteContent, type: 'INTERNE' }).subscribe({
      next: note => {
        this.notes.set([...this.notes(), note]);
        this.newNoteContent = '';
      },
      error: () => this.errorMessage.set("Echec de l'ajout de la note."),
    });
  }

  // === Resolution ===

  openResolveForm(reportId: string): void {
    this.resolvingReportId.set(reportId);
    this.resolutionComment = '';
    this.selectedProofs = [];
  }

  cancelResolveForm(): void {
    this.resolvingReportId.set(null);
  }

  onProofsSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files) return;
    const files = Array.from(input.files);
    if (files.length > 3) {
      this.errorMessage.set('Trois preuves photo au maximum.');
      return;
    }
    this.selectedProofs = files;
  }

  confirmResolve(reportId: string): void {
    if (!this.resolutionComment.trim() || this.resolutionComment.trim().length < 10) {
      this.errorMessage.set('Le commentaire de résolution doit faire au moins 10 caractères.');
      return;
    }
    if (this.selectedProofs.length === 0) {
      this.errorMessage.set('Au moins une preuve photo est obligatoire.');
      return;
    }
    this.actionInProgress.set(reportId);
    this.interventionsService.resolve(reportId, this.resolutionComment, this.selectedProofs).subscribe({
      next: () => {
        this.resolvingReportId.set(null);
        this.actionInProgress.set(null);
        this.loadDashboard();
      },
      error: () => {
        this.errorMessage.set('Echec de la résolution.');
        this.actionInProgress.set(null);
      },
    });
  }

  //Deconnexion
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