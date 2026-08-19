package africa.epf.signalville_backend.domain.model;

/**
 * Statut du cycle de vie d'une intervention (le "traitement terrain" d'un signalement).
 * Ne pas confondre avec ReportStatus, qui suit le signalement lui-même : une intervention
 * est une entité liée à UN cycle d'affectation. Un signalement peut avoir plusieurs
 * interventions successives dans le temps (ex: réaffectation), mais une seule active
 * à la fois (contrainte posée en base via un index unique partiel, voir migration V6).
 */
public enum InterventionStatus {
    /** L'agent vient d'être désigné, n'a pas encore commencé. */
    AFFECTEE,
    /** L'agent a démarré le traitement sur le terrain. */
    EN_COURS,
    /** L'agent a déclaré l'incident résolu (commentaire + preuves photo). Statut terminal. */
    RESOLUE,
    /** L'intervention a été interrompue par une réaffectation à un autre agent. Statut terminal. */
    REAFFECTEE,
    /** L'intervention a été interrompue sans réaffectation immédiate. Statut terminal. */
    INTERROMPUE
}