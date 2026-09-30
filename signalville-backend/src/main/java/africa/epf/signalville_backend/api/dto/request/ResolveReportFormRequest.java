package africa.epf.signalville_backend.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolveReportFormRequest(

        @NotBlank @Size(min = 10, max = 500)
        String resolutionComment
) {
}