package africa.epf.signalville_backend.api.dto.request;

import africa.epf.signalville_backend.domain.model.Priority;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryRequest(
        @NotBlank String name,
        String description,
        String icon,
        @NotNull Priority defaultPriority,
        @NotNull @Min(1) Integer targetDelayHours,
        Boolean active) {

    /** Le contrat definit active avec un defaut a true : applique ici si absent (POST). */
    public boolean activeOrDefault() {
        return active == null || active;
    }
}