package africa.epf.signalville_backend.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateCategoryStatusRequest(@NotNull Boolean active) {
}