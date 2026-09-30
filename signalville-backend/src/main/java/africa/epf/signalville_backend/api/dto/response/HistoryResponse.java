package africa.epf.signalville_backend.api.dto.response;

import africa.epf.signalville_backend.domain.model.ReportStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record HistoryResponse(
        UUID id,
        ReportStatus previousStatus,
        ReportStatus newStatus,
        String comment,
        UserResponse actor,
        LocalDateTime changedAt
) {
}
