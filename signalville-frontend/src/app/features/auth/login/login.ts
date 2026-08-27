import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/auth/auth';
import { Role } from '../../../shared/models/api.models';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const { email, password } = this.form.getRawValue();

    this.authService.login({ email: email!, password: password! }).subscribe({
      next: (response) => {
        this.isSubmitting.set(false);
        this.router.navigate([this.landingRouteFor(response.user.role)]);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        if (err.status === 401) {
          this.errorMessage.set("L'adresse e-mail ou le mot de passe est incorrect.");
        } else {
          this.errorMessage.set("Une erreur est survenue. Veuillez réessayer.");
        }
      },
    });
  }

  // Point d'entree post-login selon le role. CITOYEN garde le dashboard
  // existant (S2) ; AGENT et SUPERVISEUR vont directement sur leur espace
  // de travail (Module 2). ADMINISTRATEUR n'a pas encore d'espace dedie
  // (prevu Module 3) : /dashboard reste la destination la moins mauvaise
  // en attendant, plutot qu'une page qui n'existe pas encore.
  private landingRouteFor(role: Role): string {
    switch (role) {
      case 'AGENT':
        return '/agent';
      case 'SUPERVISEUR':
        return '/supervisor';
      case 'CITOYEN':
      case 'ADMINISTRATEUR':
      default:
        return '/dashboard';
    }
  }
}