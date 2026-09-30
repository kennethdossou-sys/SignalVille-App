package africa.epf.signalville_backend.api.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Mise a jour du profil par l'utilisateur lui-meme (self-service).
 * Volontairement distinct de AdminUpdateUserRequest : ni role, ni email,
 * ni statut ne sont modifiables ici.
 */
public record UpdateProfileRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String phone
) {
}