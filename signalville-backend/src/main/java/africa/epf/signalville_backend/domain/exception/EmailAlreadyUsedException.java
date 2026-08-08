package africa.epf.signalville_backend.domain.exception;

/** Inscription avec un e-mail deja pris -> 409 (contrat OpenAPI /auth/register). */
public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException(String email) {
        super("Un compte existe deja pour l'adresse " + email);
    }
}
