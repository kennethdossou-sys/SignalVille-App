import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../../core/auth/auth';
import { ProfileService } from '../../../core/services/profile';
import { BackButton } from '../../../shared/components/back-button/back-button';

@Component({
  selector: 'app-profile-info',
  standalone: true,
  imports: [ReactiveFormsModule, BackButton],
  templateUrl: './profile-info.html',
  styleUrl: './profile-info.scss',
})
export class ProfileInfo {
  private readonly fb = inject(FormBuilder);
  private readonly profileService = inject(ProfileService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly currentUser = this.authService.currentUser;
  readonly isSubmitting = signal(false);
  readonly successMessage = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.group({
    firstName: [this.currentUser()?.firstName ?? '', [Validators.required]],
    lastName: [this.currentUser()?.lastName ?? '', [Validators.required]],
    phone: [this.currentUser()?.phone ?? '', [Validators.required]],
  });

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.successMessage.set(null);
    this.errorMessage.set(null);

    const { firstName, lastName, phone } = this.form.getRawValue();

    this.profileService.updateProfile({
      firstName: firstName!,
      lastName: lastName!,
      phone: phone!,
    }).subscribe({
      next: (updated) => {
        this.isSubmitting.set(false);
        // Synchronise le currentUser stocke localement, sinon le header
        // continuerait a afficher l'ancien prenom jusqu'au prochain login.
        this.authService.syncCurrentUser(updated);
        this.successMessage.set('Profil mis a jour.');
      },
      error: () => {
        this.isSubmitting.set(false);
        this.errorMessage.set('Impossible de mettre a jour le profil. Reessayez.');
      },
    });
  }

  goToChangePassword(): void {
    this.router.navigate(['/profile/change-password']);
  }
}