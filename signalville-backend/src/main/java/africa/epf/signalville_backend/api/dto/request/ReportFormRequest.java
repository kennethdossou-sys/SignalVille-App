package africa.epf.signalville_backend.api.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Partie textuelle du multipart/form-data pour POST et PUT /reports.
 * Correspond a CreateReportMultipart du contrat, hors champ "photos"
 * qui est recu separement en @RequestPart.
 */
public record ReportFormRequest(

        @NotBlank @Size(min = 5, max = 200)
        String title,

        @NotBlank @Size(min = 30, max = 1000)
        String description,

        @NotNull
        UUID categoryId,

        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
        Double latitude,

        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
        Double longitude,

        @NotBlank @Size(max = 300)
        String address,

        @Size(max = 120)
        String district,

        @Size(max = 120)
        String municipality
) {
}
