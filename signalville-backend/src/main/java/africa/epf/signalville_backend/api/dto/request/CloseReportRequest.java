package africa.epf.signalville_backend.api.dto.request;

import jakarta.validation.constraints.Size;

public record CloseReportRequest(

        @Size(max = 500)
        String publicComment
) {
}