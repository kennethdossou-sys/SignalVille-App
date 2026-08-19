package africa.epf.signalville_backend.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ReassignReportRequest(

        @NotNull
        UUID newAgentId,

        @NotBlank @Size(max = 500)
        String reason
) {
}