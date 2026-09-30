package africa.epf.signalville_backend.api.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String title,
        String message,
        boolean read,
        String link,
        LocalDateTime createdAt
) {
}