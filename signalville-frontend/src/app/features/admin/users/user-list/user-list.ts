import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';

import { Users } from '../../../../core/services/users';
import { AccountStatus, Role, UserResponse } from '../../../../shared/models/api.models';
// user-list.ts — ligne d'import ajoutée
import { BackButton } from '../../../../shared/components/back-button/back-button';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [FormsModule, RouterLink,BackButton],
  templateUrl: './user-list.html',
  styleUrl: './user-list.scss',
})
export class UserList implements OnInit {
  private readonly usersService = inject(Users);
  private readonly router = inject(Router);

  readonly users = signal<UserResponse[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly actionInProgress = signal<string | null>(null);

  // Pagination
  readonly currentPage = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly pageSize = 20;

  // Filtres
  filterRole: Role | '' = '';
  filterStatus: AccountStatus | '' = '';
  filterSearch = '';

  readonly roles: Role[] = ['CITOYEN', 'AGENT', 'SUPERVISEUR', 'ADMINISTRATEUR'];
  readonly statuses: AccountStatus[] = ['ACTIF', 'SUSPENDU', 'DESACTIVE'];

  // Formulaire de changement de statut (motif obligatoire selon le contrat).
  readonly changingStatusUserId = signal<string | null>(null);
  newStatus: AccountStatus = 'ACTIF';
  statusReason = '';

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.usersService
      .search({
        role: this.filterRole || undefined,
        status: this.filterStatus || undefined,
        search: this.filterSearch || undefined,
        page: this.currentPage(),
        size: this.pageSize,
      })
      .subscribe({
        next: page => {
          this.users.set(page.content);
          this.totalPages.set(page.totalPages);
          this.totalElements.set(page.totalElements);
          this.loading.set(false);
        },
        error: () => {
          this.errorMessage.set('Impossible de charger les utilisateurs.');
          this.loading.set(false);
        },
      });
  }

  applyFilters(): void {
    this.currentPage.set(0);
    this.loadUsers();
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.loadUsers();
  }

  goToEdit(userId: string): void {
    this.router.navigate(['/admin/users', userId, 'edit']);
  }

  // === Changement de statut ===

  openStatusForm(user: UserResponse): void {
    this.changingStatusUserId.set(user.id);
    this.newStatus = user.status === 'ACTIF' ? 'SUSPENDU' : 'ACTIF';
    this.statusReason = '';
  }

  cancelStatusForm(): void {
    this.changingStatusUserId.set(null);
  }

  confirmStatusChange(userId: string): void {
    if (!this.statusReason.trim()) {
      this.errorMessage.set('Un motif est obligatoire pour changer le statut.');
      return;
    }
    this.actionInProgress.set(userId);
    this.usersService.updateStatus(userId, this.newStatus, this.statusReason).subscribe({
      next: () => {
        this.changingStatusUserId.set(null);
        this.actionInProgress.set(null);
        this.loadUsers();
      },
      error: () => {
        this.errorMessage.set('Échec de la modification du statut.');
        this.actionInProgress.set(null);
      },
    });
  }
}