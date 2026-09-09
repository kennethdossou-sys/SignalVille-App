import { Component, ElementRef, OnInit, AfterViewInit, ViewChild, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import * as L from 'leaflet';

import { environment } from '../../../../environments/environment';
import { Reports } from '../../../core/services/reports';
import { CategoryResponse } from '../../../shared/models/api.models';

const DAKAR_CENTER: L.LatLngTuple = [14.6928, -17.4467];

// Règles de validation des photos, alignées sur les contraintes backend
// (voir SecurityConfig / StorageProperties : 5 Mo max, JPEG/PNG uniquement).
const MAX_PHOTOS = 3;
const MAX_PHOTO_SIZE_BYTES = 5 * 1024 * 1024; // 5 Mo
const ALLOWED_PHOTO_TYPES = ['image/jpeg', 'image/png'];

// Structure interne utilisée uniquement côté front pour afficher l'aperçu.
// On garde le File original (nécessaire pour l'upload) ET une URL d'aperçu générée localement.
interface PhotoPreview {
  file: File;
  previewUrl: string;
}

@Component({
  selector: 'app-report-new',
  imports: [ReactiveFormsModule, DecimalPipe],
  templateUrl: './report-new.html',
  styleUrl: './report-new.scss',
})
export class ReportNew implements OnInit, AfterViewInit {
  private readonly fb = inject(FormBuilder);
  private readonly reportsService = inject(Reports);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  @ViewChild('mapContainer') mapContainerRef!: ElementRef<HTMLDivElement>;
  private map?: L.Map;
  private marker?: L.Marker;

  readonly categories = signal<CategoryResponse[]>([]);
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly locationSelected = signal(false);

  // Les photos ne font PAS partie du FormGroup : un input file ne peut pas être
  // lié via formControlName de façon fiable (limitation Angular connue).
  // On les gère donc dans un signal séparé, et on les injecte manuellement
  // dans le FormData au moment de la soumission.
  readonly photos = signal<PhotoPreview[]>([]);
  readonly photoError = signal<string | null>(null);

  readonly form = this.fb.group({
    title: ['', [Validators.required, Validators.minLength(5)]],
    description: ['', [Validators.required, Validators.minLength(10)]],
    categoryId: ['', [Validators.required]],
    latitude: [null as number | null, [Validators.required]],
    longitude: [null as number | null, [Validators.required]],
    address: ['', [Validators.required]],
    district: [''],
    municipality: [''],
  });

  ngOnInit(): void {
    // Chargement des catégories au démarrage du composant (pas sur une action utilisateur),
    // pour que le <select> soit déjà peuplé quand l'utilisateur arrive sur la page.
    this.http
      .get<CategoryResponse[]>(`${environment.apiUrl}/categories`)
      .subscribe({
        next: (categories) => this.categories.set(categories),
        error: () => this.errorMessage.set('Impossible de charger les catégories.'),
      });
  }

  ngAfterViewInit(): void {
    // Leaflet a besoin que le <div> cible soit déjà présent dans le DOM réel.
    // ngAfterViewInit est le seul hook Angular qui le garantit (contrairement à ngOnInit).
    this.initMap();
  }

  private initMap(): void {
    this.map = L.map(this.mapContainerRef.nativeElement).setView(DAKAR_CENTER, 12);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 19,
    }).addTo(this.map);

    this.map.on('click', (event: L.LeafletMouseEvent) => {
      this.placeMarker(event.latlng);
    });
  }

  private placeMarker(latlng: L.LatLng): void {
    // Un seul marqueur à la fois : on déplace l'existant plutôt que d'en empiler un nouveau.
    if (this.marker) {
      this.marker.setLatLng(latlng);
    } else {
      this.marker = L.marker(latlng, { draggable: true }).addTo(this.map!);
      this.marker.on('dragend', () => {
        const position = this.marker!.getLatLng();
        this.updateCoordinates(position);
      });
    }

    this.updateCoordinates(latlng);
  }

  private updateCoordinates(latlng: L.LatLng): void {
    // patchValue (et pas setValue) : on met à jour uniquement lat/lng,
    // sans exiger de fournir tous les autres champs du formulaire.
    this.form.patchValue({
      latitude: latlng.lat,
      longitude: latlng.lng,
    });
    this.locationSelected.set(true);
  }

  // Reverse geocoding via Nominatim (OpenStreetMap) : convertit lat/lng en adresse lisible.
// Utilisé uniquement pour "Utiliser ma position" — un clic manuel laisse l'utilisateur
// taper lui-même, évite de spammer l'API à chaque clic de test sur la carte.
private reverseGeocode(latlng: L.LatLng): void {
  const url = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${latlng.lat}&lon=${latlng.lng}`;

  this.http.get<{ display_name?: string; address?: Record<string, string> }>(url).subscribe({
    next: (result) => {
      const addr = result.address ?? {};
      this.form.patchValue({
        address: result.display_name ?? this.form.get('address')?.value ?? '',
        district: addr['suburb'] ?? addr['neighbourhood'] ?? '',
        municipality: addr['city'] ?? addr['town'] ?? addr['county'] ?? '',
      });
    },
    error: () => {
      // Échec silencieux : le geocoding est un confort, pas un critère bloquant.
      // L'utilisateur garde ses coordonnées valides, il tapera l'adresse à la main.
    },
  });
}

  useMyLocation(): void {
    if (!navigator.geolocation) {
      this.errorMessage.set("La géolocalisation n'est pas disponible sur votre navigateur.");
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const latlng = L.latLng(position.coords.latitude, position.coords.longitude);
        this.map!.setView(latlng, 16);
        this.placeMarker(latlng); // réutilise la même logique que le clic manuel
        this.reverseGeocode(latlng); // ← ajouté : remplit l'adresse automatiquement

      },
      () => {
        this.errorMessage.set(
          "Impossible d'obtenir votre position. Autorisez la géolocalisation ou placez le repère manuellement."
        );
      }
    );
  }

  // Déclenché par (change) sur <input type="file">.
  // event.target est typé HTMLInputElement pour accéder à .files (FileList).
  onPhotosSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const selectedFiles = input.files ? Array.from(input.files) : [];
    this.photoError.set(null);

    for (const file of selectedFiles) {
      // Règle 1 : on n'accepte jamais plus de MAX_PHOTOS au total.
      if (this.photos().length >= MAX_PHOTOS) {
        this.photoError.set(`Vous ne pouvez ajouter que ${MAX_PHOTOS} photos maximum.`);
        break;
      }

      // Règle 2 : format autorisé (aligné sur la validation backend).
      if (!ALLOWED_PHOTO_TYPES.includes(file.type)) {
        this.photoError.set('Seuls les formats JPEG et PNG sont acceptés.');
        continue;
      }

      // Règle 3 : taille max (aligné sur StorageProperties côté backend).
      if (file.size > MAX_PHOTO_SIZE_BYTES) {
        this.photoError.set('Chaque photo doit faire moins de 5 Mo.');
        continue;
      }

      this.addPhotoPreview(file);
    }

    // Réinitialise l'input pour permettre de resélectionner le même fichier
    // si l'utilisateur le retire puis veut le rajouter (sinon le navigateur
    // ignore une sélection "identique" à la précédente).
    input.value = '';
  }

  private addPhotoPreview(file: File): void {
    // FileReader lit le contenu binaire du fichier et le convertit en URL
    // affichable directement dans un <img src="...">, sans passer par le réseau.
    const reader = new FileReader();
    reader.onload = () => {
      this.photos.update(current => [
        ...current,
        { file, previewUrl: reader.result as string },
      ]);
    };
    reader.readAsDataURL(file);
  }

  removePhoto(index: number): void {
    this.photos.update(current => current.filter((_, i) => i !== index));
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    // Règle métier : au moins 1 photo est obligatoire (contrat OpenAPI, RG-05 du cahier des charges).
    if (this.photos().length === 0) {
      this.photoError.set('Au moins une photo est requise.');
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const raw = this.form.getRawValue();

    this.reportsService
      .create({
        title: raw.title!,
        description: raw.description!,
        categoryId: raw.categoryId!,
        latitude: raw.latitude!,
        longitude: raw.longitude!,
        address: raw.address!,
        district: raw.district ?? '',
        municipality: raw.municipality ?? '',
        photos: this.photos().map(p => p.file),
      })
      .subscribe({
        next: (report) => {
          this.isSubmitting.set(false);
          // Redirection vers le détail du signalement fraîchement créé.
          this.router.navigate(['/reports', report.id]);
        },
        error: (err) => {
          this.isSubmitting.set(false);
          if (err.status === 400) {
            this.errorMessage.set('Veuillez vérifier les informations saisies.');
          } else {
            this.errorMessage.set('Une erreur est survenue. Veuillez réessayer.');
          }
        },
      });
  }
}