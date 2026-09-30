package africa.epf.signalville_backend.application.service;

import java.util.UUID;

import africa.epf.signalville_backend.api.dto.request.ChangePasswordRequest;
import africa.epf.signalville_backend.api.dto.request.LoginRequest;
import africa.epf.signalville_backend.api.dto.request.RegisterRequest;
import africa.epf.signalville_backend.api.dto.response.AuthResponse;
import africa.epf.signalville_backend.domain.model.User;

/**
 * Contrat d'authentification (strategie JWT B : access token JWT courte
 * duree, refresh token opaque persiste avec rotation). L'implementation vit
 * dans application.service.impl.AuthServiceImpl.
 */
public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    /** Rotation : l'ancien refresh token devient inutilisable des qu'un nouveau est emis. */
    AuthResponse refresh(String presentedToken);

    /** Idempotent : un token deja inconnu ou deja revoque donne quand meme succes. */
    void logout(String presentedToken);

    User requireUserById(UUID id);


    /**
     * Changement de mot de passe volontaire (utilisateur authentifie).
     * Verifie le mot de passe actuel, controle la coherence
     * newPassword/confirmNewPassword, encode le nouveau et remet
     * mustChangePassword a false s'il etait a true (compte interne cree
     * avec un mot de passe temporaire).
     */
    void changePassword(UUID userId, ChangePasswordRequest request);
}