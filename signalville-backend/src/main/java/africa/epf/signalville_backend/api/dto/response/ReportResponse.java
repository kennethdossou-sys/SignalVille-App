package africa.epf.signalville_backend.api.dto.response;

import africa.epf.signalville_backend.domain.model.Priority;
import africa.epf.signalville_backend.domain.model.ReportStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReportResponse(
        UUID id,
        String reference,
        String title,
        String description,
        ReportStatus status,
        Priority priority,
        CategoryResponse category,
        Double latitude,
        Double longitude,
        String address,
        String district,
        String municipality,
        List<PhotoResponse> photos,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
