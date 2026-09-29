import { Component, input } from '@angular/core';

@Component({
  selector: 'app-loader',
  standalone: true,
  imports: [],
  templateUrl: './loader.html',
  styleUrl: './loader.scss',
})
export class Loader {
  /**
   * fullscreen : overlay plein ecran centre, avec le grand "SV" (mode
   * navigation / chargement d'app).
   * inline : petit loader compact centre dans son conteneur parent,
   * sans le "SV" ni fond (mode listes / donnees dans une page).
   */
  readonly mode = input<'fullscreen' | 'inline'>('inline');

  /** Texte affiche sous la barre. Peut etre personnalise par ecran. */
  readonly label = input<string>('Chargement...');
}