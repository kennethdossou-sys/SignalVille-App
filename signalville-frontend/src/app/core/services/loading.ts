import { inject, Injectable, signal } from '@angular/core';
import {
  NavigationCancel,
  NavigationEnd,
  NavigationError,
  NavigationStart,
  Router,
} from '@angular/router';

/**
 * Etat de chargement global de l'application, base sur les evenements
 * du router. Un loader plein ecran est affiche entre NavigationStart et
 * NavigationEnd/Error/Cancel — utile surtout pour les navigations
 * lazy-loaded qui prennent un instant a telecharger le bundle.
 */
@Injectable({ providedIn: 'root' })
export class LoadingService {
  private readonly router = inject(Router);
  readonly isLoading = signal(false);

  constructor() {
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationStart) {
        this.isLoading.set(true);
      } else if (
        event instanceof NavigationEnd ||
        event instanceof NavigationCancel ||
        event instanceof NavigationError
      ) {
        this.isLoading.set(false);
      }
    });
  }
}