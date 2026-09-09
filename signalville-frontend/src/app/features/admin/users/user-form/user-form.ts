import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Users } from '../../../../core/services/users';
import { Role } from '../../../../shared/models/api.models';

type InternalRole = 'AGENT' | 'SUPERVISEUR' | 'ADMINISTRATEUR';

@Component({
  selector: 'app-user-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './user-form.html',
  styleUrl: './user-form.scss',
})
export class UserForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly usersService = inject(Users);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly isEditMode = signal(false);
  readonly userId = signal<string | null>(null);
  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly createdTemporaryPassword = signal<string | null>(null);
  readonly createdUserEmail = signal<string | null>(null);

  readonly internalRoles: InternalRole[] = ['AGENT', 'SUPERVISEUR', 'ADMINISTRATEUR'];

  readonly form = this.fb.group({
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.required]],
    role: ['AGENT' as InternalRole, [Validators.required]],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode.set(true);
      this.userId.set(id);
      this.loading.set(true);
      this.form.get('email')?.disable();

      this.usersService.getById(id).subscribe({
        next: user => {
          this.form.patchValue({
            firstName: user.firstName,
            lastName: user.lastName,
            email: user.email,
            phone: user.phone,
            role: user.role as InternalRole,
          });
          this.loading.set(false);
        },
        error: () => {
          this.errorMessage.set('Impossible de charger l’utilisateur.');
          this.loading.set(false);
        },
      });
    }
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);

    if (this.isEditMode()) {
      const payload = {
        firstName: this.form.value.firstName!,
        lastName: this.form.value.lastName!,
        phone: this.form.value.phone!,
        role: this.form.value.role! as Role,
      };
      this.usersService.update(this.userId()!, payload).subscribe({
        next: () => {
          this.submitting.set(false);
          this.router.navigate(['/admin/users']);
        },
        error: () => {
          this.errorMessage.set('Échec de la modification.');
          this.submitting.set(false);
        },
      });
    } else {
      const payload = {
        firstName: this.form.value.firstName!,
        lastName: this.form.value.lastName!,
        email: this.form.value.email!,
        phone: this.form.value.phone!,
        role: this.form.value.role! as InternalRole,
      };
      this.usersService.create(payload).subscribe({
        next: response => {
          this.submitting.set(false);
          this.createdTemporaryPassword.set(response.temporaryPassword);
          this.createdUserEmail.set(response.user.email);
        },
        error: () => {
          this.errorMessage.set('Échec de la création. L’adresse email est peut-être déjà utilisée.');
          this.submitting.set(false);
        },
      });
    }
  }

  goToList(): void {
    this.router.navigate(['/admin/users']);
  }
}