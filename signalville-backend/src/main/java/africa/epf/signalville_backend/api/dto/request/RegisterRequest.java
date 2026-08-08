package africa.epf.signalville_backend.api.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Aligne sur RegisterRequest du contrat OpenAPI. */
public record RegisterRequest(

        @NotBlank @Size(min = 2, max = 100)
        String firstName,

        @NotBlank @Size(min = 2, max = 100)
        String lastName,

        @NotBlank @Email @Size(max = 255)
        String email,

        @NotBlank @Size(max = 20)
        String phone,

        @NotBlank @Size(min = 8, max = 100)
        String password,

        @NotBlank
        String confirmPassword,

        @AssertTrue(message = "Les conditions d'utilisation doivent etre acceptees")
        boolean termsAccepted
) {

    @AssertTrue(message = "La confirmation ne correspond pas au mot de passe")
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(confirmPassword);
    }
}
