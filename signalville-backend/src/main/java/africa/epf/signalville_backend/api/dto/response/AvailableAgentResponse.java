package africa.epf.signalville_backend.api.dto.response;

import africa.epf.signalville_backend.domain.model.AccountStatus;

import java.util.UUID;

/**
 * Vue restreinte d'un agent, destinee au superviseur pour choisir a qui
 * affecter un signalement. Volontairement plus etroite que UserResponse :
 * pas de phone, pas de createdAt/lastLoginAt - ces informations relevent de
 * la gestion de compte, reservee a l'ADMINISTRATEUR (GET /users).
 */
public record AvailableAgentResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        AccountStatus status,
        long activeInterventionsCount
) {
}