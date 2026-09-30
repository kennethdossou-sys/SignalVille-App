package africa.epf.signalville_backend.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank @Email
        String email,

        @NotBlank
        String password
) {
}
