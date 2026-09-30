package africa.epf.signalville_backend.domain.model;

/**
 * Visibilité d'une note attachée à un signalement pendant son traitement.
 */
public enum NoteType {
    /** Visible uniquement par AGENT / SUPERVISEUR / ADMINISTRATEUR. Jamais par le citoyen. */
    INTERNE,
    /** Visible également par le citoyen propriétaire du signalement. */
    PUBLIC
}