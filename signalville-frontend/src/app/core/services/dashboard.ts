import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AdminDashboardResponse,
  AgentDashboardResponse,
  CitizenDashboardResponse,
  SupervisorDashboardResponse,
} from '../../shared/models/api.models';

/**
 * Centralise les 4 tableaux de bord par rôle (GET /dashboard/*). Auparavant
 * getSupervisorDashboard vivait dans Interventions — migre ici pour que
 * tous les appels dashboard soient au meme endroit, quel que soit le role
 */
@Injectable({ providedIn: 'root' })
export class Dashboards {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/dashboard`;

  getCitizenDashboard(): Observable<CitizenDashboardResponse> {
    return this.http.get<CitizenDashboardResponse>(`${this.baseUrl}/citizen`);
  }

  getSupervisorDashboard(): Observable<SupervisorDashboardResponse> {
    return this.http.get<SupervisorDashboardResponse>(`${this.baseUrl}/supervisor`);
  }

  getAgentDashboard(): Observable<AgentDashboardResponse> {
    return this.http.get<AgentDashboardResponse>(`${this.baseUrl}/agent`);
  }

  getAdminDashboard(): Observable<AdminDashboardResponse> {
    return this.http.get<AdminDashboardResponse>(`${this.baseUrl}/admin`);
  }
}