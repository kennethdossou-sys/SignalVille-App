package africa.epf.signalville_backend.api.dto.request;

import africa.epf.signalville_backend.domain.model.AccountStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull AccountStatus status,
        @NotBlank String reason
) {
}