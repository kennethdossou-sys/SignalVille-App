import { Component, ElementRef, OnInit, AfterViewInit, ViewChild, inject, signal, effect } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import * as L from 'leaflet';

import { environment } from '../../../../environments/environment';
import { Reports } from '../../../core/services/reports';
import { CategoryResponse, ReportResponse, ReportStatus } from '../../../shared/models/api.models';

const DAKAR_CENTER: L.LatLngTuple = [14.6928, -17.4467];

// Couleurs par statut, cohérentes avec les badges déjà utilisés dans
// citizen-dashboard.scss (.status-NOUVEAU, .status-AFFECTE, etc.).
const STATUS_COLORS: Record<ReportStatus, string> = {
  NOUVEAU: '#1e40af',
  AFFECTE: '#6d28d9',
  EN_COURS: '#c2410c',
  RESOLU: '#15803d',
  CLOTURE: '#374151',
  REOUVERT: '#a16207',
  REJETE: '#b91c1c',
  ANNULE: '#9ca3af',
};

@Component({
  selector: 'app-supervisor-map',
  standalone: true,
  imports: [RouterLink, FormsModule],
  templateUrl: './supervisor-map.html',
  styleUrl: './supervisor-map.scss',
})
export class SupervisorMap implements OnInit, AfterViewInit {
  private readonly reportsService = inject(Reports);
  private readonly http = inject(HttpClient);

  @ViewChild('mapContainer') mapContainerRef!: ElementRef<HTMLDivElement>;
  private map?: L.Map;
  private markers: L.Marker[] = [];

  readonly categories = signal<CategoryResponse[]>([]);
  readonly reports = signal<ReportResponse[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly statuses: ReportStatus[] = [
    'NOUVEAU', 'AFFECTE', 'EN_COURS', 'RESOLU', 'CLOTURE', 'REOUVERT', 'REJETE', 'ANNULE',
  ];

  filterStatus: ReportStatus | '' = '';
  filterCategoryId = '';

  private mapReady = false;

  ngOnInit(): void {
    this.http
      .get<CategoryResponse[]>(`${environment.apiUrl}/categories`)
      .subscribe({
        next: categories => this.categories.set(categories),
        error: () => this.errorMessage.set('Impossible de charger les catégories.'),
      });

    this.loadReports();
  }

  ngAfterViewInit(): void {
    this.initMap();
    this.mapReady = true;
    // Si les signalements etaient deja charges avant que la carte soit prete
    // (course possible entre ngOnInit et ngAfterViewInit), on replace les
    // marqueurs maintenant.
    this.refreshMarkers();
  }

  private initMap(): void {
    this.map = L.map(this.mapContainerRef.nativeElement).setView(DAKAR_CENTER, 12);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 19,
    }).addTo(this.map);
  }

  loadReports(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.reportsService
      .list({
        status: this.filterStatus || undefined,
        categoryId: this.filterCategoryId || undefined,
        size: 200,
      })
      .subscribe({
        next: page => {
          this.reports.set(page.content);
          this.loading.set(false);
          this.refreshMarkers();
        },
        error: () => {
          this.errorMessage.set('Impossible de charger les signalements.');
          this.loading.set(false);
        },
      });
  }

  applyFilters(): void {
    this.loadReports();
  }

  private refreshMarkers(): void {
    if (!this.mapReady || !this.map) return;

    // On repart de zero a chaque rafraichissement : plus simple et fiable
    // que de faire un diff, vu le volume attendu (quelques centaines max).
    this.markers.forEach(marker => marker.remove());
    this.markers = [];

    for (const report of this.reports()) {
      const color = STATUS_COLORS[report.status] ?? '#6b7280';
      const icon = L.divIcon({
        className: 'status-marker',
        html: `<span style="background:${color}"></span>`,
        iconSize: [16, 16],
      });

      const marker = L.marker([report.latitude, report.longitude], { icon }).addTo(this.map);

      marker.bindPopup(`
        <strong>${report.reference}</strong><br/>
        ${report.title}<br/>
        <span style="color:${color}">${report.status}</span><br/>
        ${report.category.name} — ${report.address}<br/>
        <a href="/reports/${report.id}">Voir le détail</a>
      `);

      this.markers.push(marker);
    }
  }
}