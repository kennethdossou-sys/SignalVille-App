import { Component, signal, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { Header } from './shared/components/header/header';
import { Footer } from './shared/components/footer/footer';
import { LoadingService } from './core/services/loading';
import { Loader } from './shared/components/loader/loader';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, Header, Footer, Loader],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly title = signal('signalville-frontend');
  protected readonly loadingService = inject(LoadingService);
}