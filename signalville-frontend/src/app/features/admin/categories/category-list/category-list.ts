import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { Categories } from '../../../../core/services/categories';
import { CategoryResponse } from '../../../../shared/models/api.models';
import { BackButton } from '../../../../shared/components/back-button/back-button';

@Component({
  selector: 'app-category-list',
  standalone: true,
  imports: [RouterLink, BackButton],
  templateUrl: './category-list.html',
  styleUrl: './category-list.scss',
})
export class CategoryList implements OnInit {
  private readonly categoriesService = inject(Categories);
  private readonly router = inject(Router);

  readonly categories = signal<CategoryResponse[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly actionInProgress = signal<string | null>(null);

  ngOnInit(): void {
    this.loadCategories();
  }

  private loadCategories(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.categoriesService.getAll().subscribe({
      next: categories => {
        this.categories.set(categories);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les catégories.');
        this.loading.set(false);
      },
    });
  }

  toggleStatus(category: CategoryResponse): void {
    this.actionInProgress.set(category.id);
    this.categoriesService.updateStatus(category.id, !category.active).subscribe({
      next: () => {
        this.actionInProgress.set(null);
        this.loadCategories();
      },
      error: () => {
        this.errorMessage.set("Échec de la mise à jour du statut.");
        this.actionInProgress.set(null);
      },
    });
  }

  goToEdit(categoryId: string): void {
    this.router.navigate(['/admin/categories', categoryId, 'edit']);
  }
}