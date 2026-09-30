package africa.epf.signalville_backend.domain.exception;

/** Ressource inexistante -> 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String resource, Object id) {
        return new ResourceNotFoundException(resource + " introuvable : " + id);
    }
}
