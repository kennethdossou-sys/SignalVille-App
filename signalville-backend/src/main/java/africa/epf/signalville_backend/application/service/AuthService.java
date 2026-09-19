package africa.epf.signalville_backend.application.service;

import africa.epf.signalville_backend.api.dto.request.LoginRequest;
import africa.epf.signalville_backend.api.dto.request.RegisterRequest;
import africa.epf.signalville_backend.api.dto.response.AuthResponse;
import africa.epf.signalville_backend.domain.model.User;

import java.util.UUID;

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
}