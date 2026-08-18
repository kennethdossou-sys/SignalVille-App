import { Component, ElementRef, OnInit, ViewChild, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import * as L from 'leaflet';

import { Reports } from '../reports';
import { ReportDetailResponse } from '../../../shared/models/api.models';

// Photo une fois téléchargée et convertie en URL locale affichable.
interface DisplayPhoto {
  id: string;
  objectUrl: string;
}

@Component({
  selector: 'app-report-detail',
  imports: [RouterLink, DatePipe, FormsModule],
  templateUrl: './report-detail.html',
  styleUrl: './report-detail.scss',
})
export class ReportDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly reportsService = inject(Reports);

  // Optionnel : le <div> n'existe dans le DOM qu'une fois le signalement
  // chargé (il est derrière un @if dans le template).
  @ViewChild('mapContainer') mapContainerRef?: ElementRef<HTMLDivElement>;
  private map?: L.Map;

  readonly report = signal<ReportDetailResponse | null>(null);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly displayPhotos = signal<DisplayPhoto[]>([]);

  // Panneau d'annulation : masqué par défaut, affiché sur clic explicite
  // pour éviter une annulation accidentelle en un seul clic.
  readonly showCancelForm = signal(false);
  readonly isCancelling = signal(false);
  cancelReason = '';

  private reportId = '';

  ngOnInit(): void {
    this.reportId = this.route.snapshot.paramMap.get('id') ?? '';
    this.loadReport();
  }

  private loadReport(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.reportsService.getById(this.reportId).subscribe({
      next: (report) => {
        this.report.set(report);
        this.isLoading.set(false);
        this.loadPhotos(report.photos.map(p => p.id));

        // setTimeout(0) : on attend le prochain cycle de rendu pour être sûr
        // que le <div #mapContainer> (conditionné par le @if sur `report()`)
        // est bien présent dans le DOM avant que Leaflet ne s'y attache.
        setTimeout(() => this.initStaticMap(report.latitude, report.longitude), 0);
      },
      error: (err) => {
        this.isLoading.set(false);
        if (err.status === 403) this.errorMessage.set("Vous n'avez pas accès à ce signalement.");
        else if (err.status === 404) this.errorMessage.set('Ce signalement est introuvable.');
        else this.errorMessage.set('Impossible de charger ce signalement.');
      },
    });
  }

  // Chaque photo est protégée par JWT : téléchargement en Blob (l'intercepteur
  // ajoute le Bearer automatiquement), puis conversion en URL locale affichable.
  private loadPhotos(photoIds: string[]): void {
    for (const id of photoIds) {
      this.reportsService.getPhotoBlob(id).subscribe({
        next: (blob) => {
          const objectUrl = URL.createObjectURL(blob);
          this.displayPhotos.update(current => [...current, { id, objectUrl }]);
        },
        error: () => {
          // Une photo en échec n'empêche pas l'affichage des autres.
        },
      });
    }
  }

  private initStaticMap(lat: number, lng: number): void {
    if (!this.mapContainerRef) return;

    const position: L.LatLngTuple = [lat, lng];

    // Carte purement informative ici (contrairement au formulaire de création) :
    // toutes les interactions sont désactivées, on ne modifie plus la position.
    this.map = L.map(this.mapContainerRef.nativeElement, {
      dragging: false,
      scrollWheelZoom: false,
      doubleClickZoom: false,
      zoomControl: false,
    }).setView(position, 15);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 19,
    }).addTo(this.map);

    L.marker(position).addTo(this.map);
  }

  openCancelForm(): void {
    this.showCancelForm.set(true);
  }

  closeCancelForm(): void {
    this.showCancelForm.set(false);
    this.cancelReason = '';
  }

  confirmCancel(): void {
    if (!this.cancelReason.trim()) return;

    this.isCancelling.set(true);

    this.reportsService.cancel(this.reportId, this.cancelReason).subscribe({
      next: () => {
        // On recharge le signalement plutôt que de rediriger : l'utilisateur
        // voit immédiatement le nouveau statut (ANNULE) sur cette même page.
        this.isCancelling.set(false);
        this.showCancelForm.set(false);
        this.loadReport();
      },
      error: () => {
        this.isCancelling.set(false);
        this.errorMessage.set("L'annulation a échoué. Veuillez réessayer.");
      },
    });
  }
}