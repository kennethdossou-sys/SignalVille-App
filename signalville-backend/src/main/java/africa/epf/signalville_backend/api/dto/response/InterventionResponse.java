package africa.epf.signalville_backend.api.dto.response;

import africa.epf.signalville_backend.domain.model.InterventionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record InterventionResponse(
        UUID id,
        UUID reportId,
        UserResponse agent,
        InterventionStatus status,
        LocalDateTime assignedAt,
        LocalDateTime startedAt,
        LocalDateTime resolvedAt,
        String instruction,
        String resolutionComment,
        List<PhotoResponse> proofs
) {
}