package africa.epf.signalville_backend.domain.exception;

/** Action incompatible avec le statut courant -> 409 (contrat OpenAPI PUT /reports/{id}). */
public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
