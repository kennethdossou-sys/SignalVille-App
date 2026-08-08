package africa.epf.signalville_backend.domain.model;

/**
 * Statuts du cycle de vie d'un signalement, tels que definis par le contrat OpenAPI.
 * Aucun statut supplementaire ne doit etre ajoute sans mise a jour du contrat.
 */
public enum ReportStatus {
    NOUVEAU,
    AFFECTE,
    EN_COURS,
    RESOLU,
    CLOTURE,
    REOUVERT,
    REJETE,
    ANNULE
}
