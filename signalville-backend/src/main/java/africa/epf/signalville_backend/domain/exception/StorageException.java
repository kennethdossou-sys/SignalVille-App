package africa.epf.signalville_backend.domain.exception;

/** Echec d'ecriture ou de lecture d'un fichier sur le disque -> 500. */
public class StorageException extends RuntimeException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
