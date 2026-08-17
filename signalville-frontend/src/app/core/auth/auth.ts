import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AuthResponse,
  LoginRequest,
  RefreshTokenRequest,
  RegisterRequest,
  UserResponse,
} from '../../shared/models/api.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  // === Dépendances injectées ===
  private readonly http = inject(HttpClient);

  // === Constantes internes ===
  private readonly baseUrl = `${environment.apiUrl}/auth`;
  private readonly ACCESS_TOKEN_KEY = 'signalville_access_token';
  private readonly REFRESH_TOKEN_KEY = 'signalville_refresh_token';
  private readonly USER_KEY = 'signalville_user';

// === État réactif exposé ===
  readonly currentUser = signal<UserResponse | null>(null);

  constructor() {
  this.loadUserFromStorage();
}

// ============================================
// Méthodes privées — chargement initial
// ============================================

private loadUserFromStorage(): void {
  const userJson = localStorage.getItem(this.USER_KEY);
  if (userJson) {
    try {
      const user = JSON.parse(userJson) as UserResponse;
      this.currentUser.set(user);
    } catch {
      this.clearStorage();
    }
  }
}


  // ============================================
// Méthodes publiques — appels HTTP
// ============================================

register(request: RegisterRequest): Observable<AuthResponse> {
  return this.http.post<AuthResponse>(`${this.baseUrl}/register`, request).pipe(
    tap(response => this.storeAuthResponse(response))
  );
}

login(request: LoginRequest): Observable<AuthResponse> {
  return this.http.post<AuthResponse>(`${this.baseUrl}/login`, request).pipe(
    tap(response => this.storeAuthResponse(response))
  );
}

refresh(): Observable<AuthResponse> {
  const refreshToken = this.getRefreshToken();
  if (!refreshToken) {
    throw new Error('No refresh token available');
  }
  const request: RefreshTokenRequest = { refreshToken };
  return this.http.post<AuthResponse>(`${this.baseUrl}/refresh`, request).pipe(
    tap(response => this.storeAuthResponse(response))
  );
}

logout(): Observable<void> {
  const refreshToken = this.getRefreshToken();
  const request: RefreshTokenRequest = { refreshToken: refreshToken ?? '' };
  return this.http.post<void>(`${this.baseUrl}/logout`, request).pipe(
    tap(() => this.clearStorage())
  );
}

forceLocalLogout(): void {
  this.clearStorage();
}


// ============================================
// Méthodes privées — stockage localStorage

private storeAuthResponse(response: AuthResponse): void {
  localStorage.setItem(this.ACCESS_TOKEN_KEY, response.accessToken);
  localStorage.setItem(this.REFRESH_TOKEN_KEY, response.refreshToken);
  localStorage.setItem(this.USER_KEY, JSON.stringify(response.user));
  this.currentUser.set(response.user);
}

private clearStorage(): void {
  localStorage.removeItem(this.ACCESS_TOKEN_KEY);
  localStorage.removeItem(this.REFRESH_TOKEN_KEY);
  localStorage.removeItem(this.USER_KEY);
  this.currentUser.set(null);
}

getAccessToken(): string | null {
  return localStorage.getItem(this.ACCESS_TOKEN_KEY);
}

getRefreshToken(): string | null {
  return localStorage.getItem(this.REFRESH_TOKEN_KEY);
}
}