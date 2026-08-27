import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink,Router } from '@angular/router';

import { Reports } from '../../reports/reports';
import { Interventions } from '../interventions';
import { ReportResponse, NoteResponse } from '../../../shared/models/api.models';
import { AuthService } from '../../../core/auth/auth';

@Component({
  selector: 'app-agent-dashboard',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './agent-dashboard.html',
  styleUrl: './agent-dashboard.scss',
})
export class AgentDashboard implements OnInit {
  private readonly reportsService = inject(Reports);
  private readonly interventionsService = inject(Interventions);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  //Recuperer les infos de lutilisateur 
  readonly currentUser = this.authService.currentUser;

  // Signalements affectes a l'agent connecte : le backend filtre deja par
  // JWT sur GET /reports pour un role AGENT (meme mecanisme que CITOYEN,
  // cf. ReportService.list). On combine AFFECTE + EN_COURS.
  readonly assignedReports = signal<ReportResponse[]>([]);
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
    this.loadAssigned();
  }

  private loadAssigned(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    // Deux requetes puisque le contrat GET /reports ne permet qu'un seul
    // statut a la fois ; on les fusionne cote client.
    this.reportsService.list({ status: 'AFFECTE', size: 50 }).subscribe({
      next: pageAffecte => {
        this.reportsService.list({ status: 'EN_COURS', size: 50 }).subscribe({
          next: pageEnCours => {
            this.assignedReports.set([...pageAffecte.content, ...pageEnCours.content]);
            this.loading.set(false);
          },
          error: () => {
            this.errorMessage.set('Impossible de charger les signalements en cours.');
            this.loading.set(false);
          },
        });
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les signalements affectés.');
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
        this.loadAssigned();
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
        this.loadAssigned();
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