import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { UserResponse,UpdateProfileRequest,ChangePasswordRequest
 } from '../../shared/models/api.models';


@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);

  private readonly usersUrl = `${environment.apiUrl}/users`;
  private readonly authUrl = `${environment.apiUrl}/auth`;

  /** GET /users/me — profil de l'utilisateur connecte. */
  getMyProfile(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.usersUrl}/me`);
  }

  /** PATCH /users/me — mise a jour self-service (firstName/lastName/phone). */
  updateProfile(request: UpdateProfileRequest): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.usersUrl}/me`, request);
  }

  /**
   * PATCH /auth/password — changement volontaire du mot de passe.
   * Cote backend : verifie currentPassword, controle newPassword ==
   * confirmNewPassword, remet mustChangePassword a false, et revoque
   * tous les refresh tokens actifs. Le frontend doit donc rediriger
   * vers /login juste apres.
   */
  changePassword(request: ChangePasswordRequest): Observable<void> {
    return this.http.patch<void>(`${this.authUrl}/password`, request);
  }
}