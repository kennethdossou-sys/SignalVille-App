import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Reports } from '../reports';
import { ReportResponse, ReportStatus } from '../../../shared/models/api.models';

// Statuts affichables dans le filtre, avec libellé français.
// Gardé local au composant : utilisé nulle part ailleurs pour l'instant.
const STATUS_OPTIONS: { value: ReportStatus; label: string }[] = [
  { value: 'NOUVEAU', label: 'Nouveau' },
  { value: 'AFFECTE', label: 'Affecté' },
  { value: 'EN_COURS', label: 'En cours' },
  { value: 'RESOLU', label: 'Résolu' },
  { value: 'CLOTURE', label: 'Clôturé' },
  { value: 'REOUVERT', label: 'Rouvert' },
  { value: 'REJETE', label: 'Rejeté' },
  { value: 'ANNULE', label: 'Annulé' },
];

@Component({
  selector: 'app-report-list',
  imports: [RouterLink, FormsModule, DatePipe],
  templateUrl: './report-list.html',
  styleUrl: './report-list.scss',
})
export class ReportList implements OnInit {
  private readonly reportsService = inject(Reports);

  readonly statusOptions = STATUS_OPTIONS;

  readonly reports = signal<ReportResponse[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  // Filtres liés au template via [(ngModel)] (template-driven, pas Reactive Form :
  // pas de validation nécessaire pour un simple filtre de recherche/statut).
  selectedStatus = '';
  searchTerm = '';

  readonly currentPage = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  private readonly pageSize = 10;

  ngOnInit(): void {
    this.loadReports();
  }

  loadReports(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.reportsService
      .list({
        page: this.currentPage(),
        size: this.pageSize,
        status: this.selectedStatus || undefined,
        search: this.searchTerm || undefined,
      })
      .subscribe({
        next: (page) => {
          this.reports.set(page.content);
          this.totalPages.set(page.totalPages);
          this.totalElements.set(page.totalElements);
          this.isLoading.set(false);
        },
        error: () => {
          this.errorMessage.set('Impossible de charger vos signalements.');
          this.isLoading.set(false);
        },
      });
  }

  // Revient toujours à la page 0 quand un filtre change, sinon on peut se
  // retrouver sur une page qui n'existe plus dans les résultats filtrés.
  onFilterChange(): void {
    this.currentPage.set(0);
    this.loadReports();
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.loadReports();
  }
}