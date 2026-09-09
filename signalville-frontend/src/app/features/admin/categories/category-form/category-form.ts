import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Categories } from '../../../../core/services/categories';
import { Priority } from '../../../../shared/models/api.models';

@Component({
  selector: 'app-category-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './category-form.html',
  styleUrl: './category-form.scss',
})
export class CategoryForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly categoriesService = inject(Categories);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly isEditMode = signal(false);
  readonly categoryId = signal<string | null>(null);
  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly priorities: Priority[] = ['BASSE', 'MOYENNE', 'HAUTE', 'CRITIQUE'];

  readonly form = this.fb.group({
    name: ['', [Validators.required]],
    description: [''],
    icon: [''],
    defaultPriority: ['MOYENNE' as Priority, [Validators.required]],
    targetDelayHours: [24, [Validators.required, Validators.min(1)]],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode.set(true);
      this.categoryId.set(id);
      this.loading.set(true);

      this.categoriesService.getById(id).subscribe({
        next: category => {
          this.form.patchValue({
            name: category.name,
            description: category.description,
            icon: category.icon,
            defaultPriority: category.defaultPriority,
            targetDelayHours: category.targetDelayHours,
          });
          this.loading.set(false);
        },
        error: () => {
          this.errorMessage.set('Impossible de charger la catégorie.');
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

    const payload = {
      name: this.form.value.name!,
      description: this.form.value.description ?? '',
      icon: this.form.value.icon ?? '',
      defaultPriority: this.form.value.defaultPriority! as Priority,
      targetDelayHours: this.form.value.targetDelayHours!,
    };

    const request$ = this.isEditMode()
      ? this.categoriesService.update(this.categoryId()!, payload)
      : this.categoriesService.create(payload);

    request$.subscribe({
      next: () => {
        this.submitting.set(false);
        this.router.navigate(['/admin/categories']);
      },
      error: () => {
        this.errorMessage.set(
          this.isEditMode()
            ? 'Échec de la modification. Le nom existe peut-être déjà.'
            : 'Échec de la création. Le nom existe peut-être déjà.'
        );
        this.submitting.set(false);
      },
    });
  }
}