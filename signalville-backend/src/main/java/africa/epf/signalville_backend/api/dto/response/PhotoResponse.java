package africa.epf.signalville_backend.api.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record PhotoResponse(
        UUID id,
        String url,
        String description,
        Integer order,
        LocalDateTime createdAt
) {
}
