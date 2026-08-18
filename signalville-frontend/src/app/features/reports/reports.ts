import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ReportDetailResponse,
  ReportPage,
  ReportResponse,
} from '../../shared/models/api.models';

// Filtres optionnels pour GET /reports (query params du contrat OpenAPI).
export interface ReportListFilters {
  page?: number;
  size?: number;
  status?: string;
  search?: string;
}

@Injectable({ providedIn: 'root' })
export class Reports {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/reports`;

  // Liste paginée. Le backend filtre déjà par citoyen connecté (via le JWT),
  // pas besoin de passer un userId ici.
  list(filters: ReportListFilters = {}): Observable<ReportPage> {
    let params = new HttpParams()
      .set('page', filters.page ?? 0)
      .set('size', filters.size ?? 20);

    // On n'ajoute les params optionnels que s'ils ont une vraie valeur,
    // pour éviter d'envoyer des query params vides au backend.
    if (filters.status) params = params.set('status', filters.status);
    if (filters.search) params = params.set('search', filters.search);

    return this.http.get<ReportPage>(this.baseUrl, { params });
  }

  // Détail complet (inclut historique, citoyen, intervention active).
  getById(id: string): Observable<ReportDetailResponse> {
    return this.http.get<ReportDetailResponse>(`${this.baseUrl}/${id}`);
  }

  // Création multipart (champs texte + fichiers photos).
  create(payload: {
    title: string;
    description: string;
    categoryId: string;
    latitude: number;
    longitude: number;
    address: string;
    district: string;
    municipality: string;
    photos: File[];
  }): Observable<ReportResponse> {
    const formData = new FormData();
    formData.append('title', payload.title);
    formData.append('description', payload.description);
    formData.append('categoryId', payload.categoryId);
    formData.append('latitude', payload.latitude.toString());
    formData.append('longitude', payload.longitude.toString());
    formData.append('address', payload.address);
    formData.append('district', payload.district);
    formData.append('municipality', payload.municipality);
    payload.photos.forEach(photo => formData.append('photos', photo, photo.name));

    return this.http.post<ReportResponse>(this.baseUrl, formData);
  }

  // Annulation, valide uniquement côté backend si le statut est encore NOUVEAU.
  // Le front n'a qu'à afficher/masquer le bouton en fonction du statut connu.
  cancel(id: string, reason: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${id}/cancel`, { reason });
  }

  // Endpoint photo authentifié : un <img src="..."> classique ne peut pas
  // porter le header Authorization. On récupère donc le fichier en Blob via
  // HttpClient (intercepté par authInterceptor), à convertir ensuite en URL
  // locale affichable avec URL.createObjectURL().
  getPhotoBlob(photoId: string): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/photos/${photoId}`, { responseType: 'blob' });
  }
}