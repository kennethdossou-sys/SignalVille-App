package africa.epf.signalville_backend.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelReportRequest(

        @NotBlank @Size(max = 500)
        String reason
) {
}
