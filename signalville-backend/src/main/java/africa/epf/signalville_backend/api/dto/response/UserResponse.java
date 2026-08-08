package africa.epf.signalville_backend.api.dto.response;

import africa.epf.signalville_backend.domain.model.AccountStatus;
import africa.epf.signalville_backend.domain.model.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        Role role,
        AccountStatus status,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {
}
