package africa.epf.signalville_backend.api.dto.response;

import africa.epf.signalville_backend.domain.model.NoteType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NoteResponse(
        UUID id,
        String content,
        NoteType type,
        UserResponse author,
        LocalDateTime createdAt
) {
}