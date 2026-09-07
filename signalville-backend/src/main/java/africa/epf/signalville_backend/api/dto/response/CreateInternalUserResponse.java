package africa.epf.signalville_backend.api.dto.response;

/**
 * Reponse dediee a POST /users, distincte de UserResponse : le mot de passe
 * temporaire n'apparait que dans cette reponse, une seule fois, jamais
 * renvoye par une autre route (GET /users, GET /users/{id}, etc.).
 */
public record CreateInternalUserResponse(
        UserResponse user,
        String temporaryPassword
) {
}