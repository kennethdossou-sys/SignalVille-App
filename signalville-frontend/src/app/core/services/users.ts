import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AccountStatus,
  AdminUpdateUserRequest,
  CreateInternalUserRequest,
  CreateInternalUserResponse,
  Role,
  UserPage,
  UserResponse,
} from '../../shared/models/api.models';

export interface UserSearchParams {
  role?: Role;
  status?: AccountStatus;
  search?: string;
  page?: number;
  size?: number;
}

/**
 * CRUD des comptes internes (AGENT, SUPERVISEUR, ADMINISTRATEUR), reserve a
 * l'administrateur (Module 3). create() retourne un mot de passe temporaire
 * en clair, affiche une seule fois cote UI (decision Seance 4, option d2) —
 * ne jamais le persister ni le reafficher ailleurs.
 *
 * Depuis Seance 5 : create() accepte sendByEmail et retourne emailSent /
 * emailError pour signaler l'issue de l'envoi Mailtrap sans bloquer la
 * creation.
 */
@Injectable({ providedIn: 'root' })
export class Users {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/users`;

  search(params: UserSearchParams): Observable<UserPage> {
    let httpParams = new HttpParams();
    if (params.role) httpParams = httpParams.set('role', params.role);
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.search) httpParams = httpParams.set('search', params.search);
    httpParams = httpParams.set('page', params.page ?? 0);
    httpParams = httpParams.set('size', params.size ?? 20);

    return this.http.get<UserPage>(this.baseUrl, { params: httpParams });
  }

  getById(id: string): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.baseUrl}/${id}`);
  }

  create(payload: CreateInternalUserRequest): Observable<CreateInternalUserResponse> {
    return this.http.post<CreateInternalUserResponse>(this.baseUrl, payload);
  }

  update(id: string, payload: AdminUpdateUserRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.baseUrl}/${id}`, payload);
  }

  updateStatus(id: string, status: AccountStatus, reason: string): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.baseUrl}/${id}/status`, { status, reason });
  }
}