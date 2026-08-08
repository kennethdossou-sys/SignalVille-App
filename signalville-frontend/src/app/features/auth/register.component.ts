import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';

/** Miroir de la contrainte serveur : password === confirmPassword. */
function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value;
  const confirmPassword = group.get('confirmPassword')?.value;
  return password === confirmPassword ? null : { passwordMismatch: true };
}

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="card">
      <h1>Creer un compte citoyen</h1>

      <form [formGroup]="form" (ngSubmit)="submit()">
        <div class="row">
          <label>
            Prenom
            <input formControlName="firstName" />
          </label>
          <label>
            Nom
            <input formControlName="lastName" />
          </label>
        </div>

        <label>
          E-mail
          <input type="email" formControlName="email" autocomplete="email" />
        </label>

        <label>
          Telephone
          <input formControlName="phone" placeholder="+229 97 00 00 00" />
        </label>

        <div class="row">
          <label>
            Mot de passe
            <input type="password" formControlName="password" autocomplete="new-password" />
          </label>
          <label>
            Confirmation
            <input type="password" formControlName="confirmPassword" autocomplete="new-password" />
          </label>
        </div>

        @if (form.hasError('passwordMismatch') && form.get('confirmPassword')?.touched) {
          <p class="error">Les deux mots de passe ne correspondent pas.</p>
        }

        <label class="checkbox">
          <input type="checkbox" formControlName="termsAccepted" />
          J'accepte les conditions d'utilisation
        </label>

        @if (error()) {
          <p class="error">{{ error() }}</p>
        }

        <button type="submit" [disabled]="form.invalid || loading()">
          {{ loading() ? 'Creation...' : 'Creer mon compte' }}
        </button>
      </form>

      <p class="hint">Deja inscrit ? <a routerLink="/login">Se connecter</a></p>
    </section>
  `,
  styleUrl: './auth.scss',
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group(
    {
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
      termsAccepted: [false, [Validators.requiredTrue]],
    },
    { validators: passwordsMatch },
  );

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);

    this.auth.register(this.form.getRawValue()).subscribe({
      next: () => {
        this.loading.set(false);
        void this.router.navigate(['/dashboard']);
      },
      error: (response: HttpErrorResponse) => {
        this.loading.set(false);
        if (response.status === 409) {
          this.error.set('Un compte existe deja avec cette adresse e-mail.');
        } else if (response.status === 400) {
          this.error.set(formatValidationErrors(response));
        } else {
          this.error.set('Le service est indisponible. Verifiez que le backend tourne sur le port 8080.');
        }
      },
    });
  }
}

function formatValidationErrors(response: HttpErrorResponse): string {
  const errors = response.error?.validationErrors as Record<string, string> | undefined;
  if (!errors) {
    return response.error?.message ?? 'Formulaire invalide.';
  }
  return Object.entries(errors)
    .map(([field, message]) => `${field} : ${message}`)
    .join(' | ');
}
