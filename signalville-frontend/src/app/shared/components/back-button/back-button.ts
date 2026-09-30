import { Component, inject } from '@angular/core';
import { Location } from '@angular/common';

@Component({
  selector: 'app-back-button',
  standalone: true,
  imports: [],
  templateUrl: './back-button.html',
  styleUrl: './back-button.scss',
})
export class BackButton {
  private readonly location = inject(Location);

  goBack(): void {
    this.location.back();
  }
}