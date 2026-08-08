import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest, UserResponse } from '../../shared/models/api.models';

const ACCESS_TOKEN_KEY = 'signalville.accessToken';
const REFRESH_TOKEN_KEY = 'signalville.refreshToken';
const USER_KEY = 'signalville.user';

/**
 * Etat d'authentification du front.
 *
 * Choix de stockage : localStorage pour les deux tokens.
 * Le cookie httpOnly serait plus sur contre le XSS, mais il impose que le
 * backend pose lui-meme le cookie (Set-Cookie + SameSite=None + HTTPS) alors
 * que le contrat OpenAPI renvoie explicitement refreshToken dans le corps de
 * AuthResponse. On respecte le contrat, et on limite l'exposition autrement :
 * access token de 15 min, refresh rotatif et revocable en base.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  private readonly userSignal = signal<UserResponse | null>(readStoredUser());

  readonly currentUser = this.userSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.userSignal() !== null);

  get accessToken(): string | null {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  }

  get refreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  register(payload: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/auth/register`, payload)
      .pipe(tap((response) => this.persist(response)));
  }

  login(payload: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/auth/login`, payload)
      .pipe(tap((response) => this.persist(response)));
  }

  refresh(): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/auth/refresh`, { refreshToken: this.refreshToken })
      .pipe(tap((response) => this.persist(response)));
  }

  /** Revoque le refresh token cote serveur avant de vider l'etat local. */
  logout(): Observable<void> {
    const refreshToken = this.refreshToken;
    return this.http
      .post<void>(`${this.baseUrl}/auth/logout`, { refreshToken })
      .pipe(tap({ next: () => this.clear(), error: () => this.clear() }));
  }

  loadCurrentUser(): Observable<UserResponse> {
    return this.http
      .get<UserResponse>(`${this.baseUrl}/users/me`)
      .pipe(tap((user) => this.userSignal.set(user)));
  }

  clear(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.userSignal.set(null);
  }

  private persist(response: AuthResponse): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(response.user));
    this.userSignal.set(response.user);
  }
}

function readStoredUser(): UserResponse | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as UserResponse;
  } catch {
    return null;
  }
}
