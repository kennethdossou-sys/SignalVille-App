package africa.epf.signalville_backend.domain.exception;

/** Regle metier violee cote requete -> 400. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
