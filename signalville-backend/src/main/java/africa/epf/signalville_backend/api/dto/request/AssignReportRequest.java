package africa.epf.signalville_backend.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AssignReportRequest(

        @NotNull
        UUID agentId,

        @Size(max = 500)
        String instruction
) {
}