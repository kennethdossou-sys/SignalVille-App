package africa.epf.signalville_backend.api.dto.response;

import africa.epf.signalville_backend.domain.model.Priority;

import java.time.LocalDateTime;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String description,
        String icon,
        Priority defaultPriority,
        Integer targetDelayHours,
        boolean active,
        LocalDateTime createdAt
) {
}
