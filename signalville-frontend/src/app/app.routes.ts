import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth-guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login').then(m => m.Login),
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register').then(m => m.Register),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/dashboard/dashboard').then(m => m.Dashboard),
  },
  {
    path: 'reports/new',
    canActivate: [authGuard],
    loadComponent: () => import('./features/reports/report-new/report-new').then(m => m.ReportNew),
  },
  {
    path: 'reports',
    canActivate: [authGuard],
    loadComponent: () => import('./features/reports/report-list/report-list').then(m => m.ReportList),
  },
  {
    path: 'reports/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./features/reports/report-detail/report-detail').then(m => m.ReportDetail),
  },
  {
  path: 'supervisor',
  loadComponent: () => import('./features/interventions/supervisor-dashboard/supervisor-dashboard').then(m => m.SupervisorDashboard),
  canActivate: [authGuard],
},
{
  path: 'agent',
  loadComponent: () => import('./features/interventions/agent-dashboard/agent-dashboard').then(m => m.AgentDashboard),
  canActivate: [authGuard],
},
];