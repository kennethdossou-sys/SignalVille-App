import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { CategoryRequest, CategoryResponse } from '../../shared/models/api.models';

/**
 * CRUD complet des categories (Module 3, administration). Distinct de
 * l'appel public GET /categories (actives uniquement, utilise par exemple
 * dans report-new) : getAll() ici retourne actives ET inactives, reserve
 * a l'administrateur, necessaire pour retrouver une categorie desactivee.
 */
@Injectable({ providedIn: 'root' })
export class Categories {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/categories`;

  /** GET /categories/all — toutes les categories, actives et inactives. */
  getAll(): Observable<CategoryResponse[]> {
    return this.http.get<CategoryResponse[]>(`${this.baseUrl}/all`);
  }

  /** GET /categories/{id} — retourne la catégorie quel que soit son statut. */
  getById(id: string): Observable<CategoryResponse> {
    return this.http.get<CategoryResponse>(`${this.baseUrl}/${id}`);
  }

  create(payload: CategoryRequest): Observable<CategoryResponse> {
    return this.http.post<CategoryResponse>(this.baseUrl, payload);
  }

  update(id: string, payload: CategoryRequest): Observable<CategoryResponse> {
    return this.http.put<CategoryResponse>(`${this.baseUrl}/${id}`, payload);
  }

  updateStatus(id: string, active: boolean): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/${id}/status`, { active });
  }
}