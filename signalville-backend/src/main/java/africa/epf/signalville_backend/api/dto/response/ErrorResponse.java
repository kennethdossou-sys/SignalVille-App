package africa.epf.signalville_backend.api.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

/** Corps d'erreur unique de l'API, conforme au schema ErrorResponse du contrat. */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, null);
    }

    public static ErrorResponse validation(int status, String error, String message, String path,
                                           Map<String, String> validationErrors) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, validationErrors);
    }
}
