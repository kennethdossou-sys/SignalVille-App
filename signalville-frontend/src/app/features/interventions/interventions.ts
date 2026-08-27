import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AssignReportRequest,
  AvailableAgentResponse,
  InterventionResponse,
  NoteResponse,
  CreateNoteRequest,
  ReassignReportRequest,
} from '../../shared/models/api.models';

@Injectable({ providedIn: 'root' })
export class Interventions {
  private readonly http = inject(HttpClient);
  private readonly reportsUrl = `${environment.apiUrl}/reports`;
  private readonly agentsUrl = `${environment.apiUrl}/agents`;
  private readonly interventionsUrl = `${environment.apiUrl}/interventions`;

  listAvailableAgents(): Observable<AvailableAgentResponse[]> {
    return this.http.get<AvailableAgentResponse[]>(`${this.agentsUrl}/available`);
  }

  getActiveIntervention(reportId: string): Observable<InterventionResponse> {
    return this.http.get<InterventionResponse>(`${this.reportsUrl}/${reportId}/intervention`);
  }

  assign(reportId: string, payload: AssignReportRequest): Observable<InterventionResponse> {
    return this.http.post<InterventionResponse>(`${this.reportsUrl}/${reportId}/assign`, payload);
  }

  reassign(reportId: string, payload: ReassignReportRequest): Observable<InterventionResponse> {
    return this.http.post<InterventionResponse>(`${this.reportsUrl}/${reportId}/reassign`, payload);
  }

  start(reportId: string): Observable<InterventionResponse> {
    return this.http.post<InterventionResponse>(`${this.reportsUrl}/${reportId}/start`, {});
  }

  resolve(reportId: string, resolutionComment: string, proofs: File[]): Observable<InterventionResponse> {
    const formData = new FormData();
    formData.append('resolutionComment', resolutionComment);
    proofs.forEach(proof => formData.append('proofs', proof, proof.name));
    return this.http.post<InterventionResponse>(`${this.reportsUrl}/${reportId}/resolve`, formData);
  }

  close(reportId: string, publicComment?: string): Observable<void> {
    return this.http.post<void>(`${this.reportsUrl}/${reportId}/close`, { publicComment });
  }

  reopen(reportId: string, reason: string): Observable<void> {
    return this.http.post<void>(`${this.reportsUrl}/${reportId}/reopen`, { reason });
  }

  reject(reportId: string, reason: string): Observable<void> {
    return this.http.post<void>(`${this.reportsUrl}/${reportId}/reject`, { reason });
  }

  listNotes(reportId: string): Observable<NoteResponse[]> {
    return this.http.get<NoteResponse[]>(`${this.reportsUrl}/${reportId}/notes`);
  }

  addNote(reportId: string, payload: CreateNoteRequest): Observable<NoteResponse> {
    return this.http.post<NoteResponse>(`${this.reportsUrl}/${reportId}/notes`, payload);
  }

  getProofBlob(interventionId: string, proofId: string): Observable<Blob> {
    return this.http.get(`${this.interventionsUrl}/${interventionId}/proofs/${proofId}`, { responseType: 'blob' });
  }
}