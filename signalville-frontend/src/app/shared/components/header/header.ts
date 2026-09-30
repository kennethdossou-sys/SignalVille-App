import { Component, ElementRef, HostListener, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter } from 'rxjs/operators';

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
  private readonly elementRef = inject(ElementRef);

  readonly currentUser = this.authService.currentUser;
  readonly isMenuOpen = signal(false);

  constructor() {
    // Ferme le menu automatiquement lors d'une navigation reussie.
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => this.isMenuOpen.set(false));
  }

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

  toggleMenu(event: MouseEvent): void {
    event.stopPropagation();
    this.isMenuOpen.update((open) => !open);
  }

  logout(): void {
    this.isMenuOpen.set(false);
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => {
        this.authService.forceLocalLogout();
        this.router.navigate(['/login']);
      },
    });
  }

  // Ferme le menu si on clique en dehors du header (delegue au document).
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.isMenuOpen()) return;
    const target = event.target as Node;
    if (!this.elementRef.nativeElement.contains(target)) {
      this.isMenuOpen.set(false);
    }
  }

  // Ferme le menu avec Escape (accessibilite clavier).
  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.isMenuOpen()) {
      this.isMenuOpen.set(false);
    }
  }
}