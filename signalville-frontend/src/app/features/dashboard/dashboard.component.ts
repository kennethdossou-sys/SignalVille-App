import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/auth/auth.service';
import { PageResponse, ReportResponse, UserResponse } from '../../shared/models/api.models';

/**
 * Page de preuve front<->back : elle rejoue deux appels proteges par JWT
 * (GET /users/me et GET /reports) et affiche ce que le serveur renvoie.
 */
@Component({
  selector: 'app-dashboard',
  template: `
    <section class="page">
      <header>
        <h1>Espace {{ roleLabel() }}</h1>
        <button type="button" (click)="logout()">Se deconnecter</button>
      </header>

      @if (error()) {
        <p class="error">{{ error() }}</p>
      }

      @if (profile(); as user) {
        <div class="panel">
          <h2>Profil verifie cote serveur (GET /users/me)</h2>
          <dl>
            <dt>Nom</dt><dd>{{ user.firstName }} {{ user.lastName }}</dd>
            <dt>E-mail</dt><dd>{{ user.email }}</dd>
            <dt>Telephone</dt><dd>{{ user.phone }}</dd>
            <dt>Role</dt><dd>{{ user.role }}</dd>
            <dt>Statut</dt><dd>{{ user.status }}</dd>
            <dt>Derniere connexion</dt><dd>{{ user.lastLoginAt ?? '-' }}</dd>
          </dl>
        </div>
      } @else if (!error()) {
        <p>Chargement du profil...</p>
      }

      <div class="panel">
        <h2>Mes signalements (GET /reports)</h2>
        @if (reports().length === 0) {
          <p>Aucun signalement pour le moment.</p>
        } @else {
          <table>
            <thead>
              <tr>
                <th>Reference</th><th>Titre</th><th>Categorie</th><th>Statut</th><th>Priorite</th>
              </tr>
            </thead>
            <tbody>
              @for (report of reports(); track report.id) {
                <tr>
                  <td>{{ report.reference }}</td>
                  <td>{{ report.title }}</td>
                  <td>{{ report.category.name }}</td>
                  <td>{{ report.status }}</td>
                  <td>{{ report.priority }}</td>
                </tr>
              }
            </tbody>
          </table>
        }
      </div>
    </section>
  `,
  styles: `
    .page { max-width: 52rem; margin: 2rem auto; padding: 0 1rem; }
    header { display: flex; justify-content: space-between; align-items: center; gap: 1rem; }
    h1 { font-size: 1.35rem; margin: 0; }
    h2 { font-size: 1rem; margin: 0 0 0.75rem; }
    button { padding: 0.45rem 0.8rem; border: 1px solid #c4cad3; border-radius: 4px; background: #fff; font: inherit; cursor: pointer; }
    .panel { margin-top: 1.5rem; padding: 1rem; border: 1px solid #d5d9e0; border-radius: 8px; background: #fff; }
    dl { display: grid; grid-template-columns: 12rem 1fr; gap: 0.35rem 1rem; margin: 0; font-size: 0.9rem; }
    dt { color: #5a6472; }
    dd { margin: 0; }
    table { width: 100%; border-collapse: collapse; font-size: 0.9rem; }
    th, td { text-align: left; padding: 0.45rem 0.5rem; border-bottom: 1px solid #e4e7ec; }
    .error { padding: 0.6rem; border-radius: 4px; background: #fdecec; color: #a3211d; }
    @media (max-width: 34rem) { dl { grid-template-columns: 1fr; } }
  `,
})
export class DashboardComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly profile = signal<UserResponse | null>(null);
  protected readonly reports = signal<ReportResponse[]>([]);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.auth.loadCurrentUser().subscribe({
      next: (user) => this.profile.set(user),
      error: () => this.error.set('Impossible de charger le profil depuis le backend.'),
    });

    this.http
      .get<PageResponse<ReportResponse>>(`${environment.apiUrl}/reports`)
      .subscribe({ next: (page) => this.reports.set(page.content) });
  }

  protected roleLabel(): string {
    return this.auth.currentUser()?.role.toLowerCase() ?? 'utilisateur';
  }

  protected logout(): void {
    // AuthService vide l'etat local dans les deux cas : on quitte la session
    // meme si la revocation serveur echoue.
    const goToLogin = () => void this.router.navigate(['/login']);
    this.auth.logout().subscribe({ next: goToLogin, error: goToLogin });
  }
}
