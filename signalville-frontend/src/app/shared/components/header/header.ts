import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/auth/auth';
import { Role } from '../../models/api.models';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './header.html',
  styleUrl: './header.scss',
})
export class Header {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly currentUser = this.authService.currentUser;

  /**
   * Destination du lien "SignalVille" selon le role connecte. Meme mapping
   * que login.landingRouteFor(). Retourne '/login' si personne n'est
   * connecte (bandeau visible sur les pages publiques aussi).
   */
  get brandLink(): string {
    const user = this.currentUser();
    if (!user) return '/login';
    switch (user.role as Role) {
      case 'AGENT':
        return '/agent';
      case 'SUPERVISEUR':
        return '/supervisor';
      case 'ADMINISTRATEUR':
        return '/admin';
      case 'CITOYEN':
      default:
        return '/dashboard';
    }
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => {
        this.authService.forceLocalLogout();
        this.router.navigate(['/login']);
      },
    });
  }
}