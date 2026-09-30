import { Component, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../../core/auth/auth';
import { ProfileService } from '../../../core/services/profile';
import { BackButton } from '../../../shared/components/back-button/back-button';

/** Valide que newPassword et confirmNewPassword correspondent. */
const passwordsMatchValidator: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
  const newPwd = group.get('newPassword')?.value;
  const confirm = group.get('confirmNewPassword')?.value;
  return newPwd && confirm && newPwd !== confirm ? { passwordsMismatch: true } : null;
};

/** Valide que newPassword est different de currentPassword. */
const newPasswordDifferentValidator: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
  const current = group.get('currentPassword')?.value;
  const newPwd = group.get('newPassword')?.value;
  return current && newPwd && current === newPwd ? { samePassword: true } : null;
};

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [ReactiveFormsModule, BackButton],
  templateUrl: './change-password.html',
  styleUrl: './change-password.scss',
})
export class ChangePassword {
  private readonly fb = inject(FormBuilder);
  private readonly profileService = inject(ProfileService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly currentUser = this.authService.currentUser;
  readonly isForced = computed(() => this.currentUser()?.mustChangePassword ?? false);
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly showCurrent = signal(false);
  readonly showNew = signal(false);
  readonly showConfirm = signal(false);

  readonly form = this.fb.group(
    {
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmNewPassword: ['', [Validators.required]],
    },
    { validators: [passwordsMatchValidator, newPasswordDifferentValidator] }
  );

  toggleCurrent(): void { this.showCurrent.update(v => !v); }
  toggleNew(): void { this.showNew.update(v => !v); }
  toggleConfirm(): void { this.showConfirm.update(v => !v); }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const { currentPassword, newPassword, confirmNewPassword } = this.form.getRawValue();

    this.profileService.changePassword({
      currentPassword: currentPassword!,
      newPassword: newPassword!,
      confirmNewPassword: confirmNewPassword!,
    }).subscribe({
      next: () => {
        // Le backend a revoque tous les refresh tokens : on nettoie le
        // storage local et on redirige vers /login. Un toast/message avant
        // la redirection serait plus doux, mais un simple query param sur
        // /login suffit et evite d'ajouter une lib de toasts.
        this.authService.forceLocalLogout();
        this.router.navigate(['/login'], {
          queryParams: { passwordChanged: '1' },
        });
      },
      error: (err) => {
        this.isSubmitting.set(false);
        if (err.status === 401) {
          this.errorMessage.set('Mot de passe actuel invalide.');
        } else if (err.status === 400) {
          // BusinessRuleException cote backend (confirm mismatch, meme mdp).
          this.errorMessage.set(err.error?.message ?? 'Requete invalide.');
        } else {
          this.errorMessage.set('Une erreur est survenue. Reessayez.');
        }
      },
    });
  }
}