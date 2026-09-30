package africa.epf.signalville_backend.api.dto.response;

/**
 * Reponse dediee a POST /users, distincte de UserResponse : le mot de passe
 * temporaire n'apparait que dans cette reponse, une seule fois, jamais
 * renvoye par une autre route (GET /users, GET /users/{id}, etc.).
 *
 * emailSent : true si sendByEmail=true dans la requete ET l'envoi a reussi.
 * emailError : message d'erreur si l'envoi a echoue (compte cree quand meme).
 */
public record CreateInternalUserResponse(
        UserResponse user,
        String temporaryPassword,
        boolean emailSent,
        String emailError
) {
}