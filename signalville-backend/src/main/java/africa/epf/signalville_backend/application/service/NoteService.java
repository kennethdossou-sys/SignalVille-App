package africa.epf.signalville_backend.application.service;

import africa.epf.signalville_backend.domain.model.NoteType;
import africa.epf.signalville_backend.domain.model.ReportNote;
import africa.epf.signalville_backend.infrastructure.security.AppUserPrincipal;

import java.util.List;
import java.util.UUID;

/**
 * Contrat des notes libres attachees a un signalement pendant son traitement.
 * L'implementation vit dans application.service.impl.NoteServiceImpl.
 *
 * Le filtrage de visibilite (INTERNE invisible au citoyen) est fait au
 * niveau requete (ReportNoteRepository), jamais laisse au frontend a filtrer.
 */
public interface NoteService {

    ReportNote add(AppUserPrincipal principal, UUID reportId, String content, NoteType type);

    /** CITOYEN ne voit que les notes PUBLIC ; les autres roles voient tout. */
    List<ReportNote> list(AppUserPrincipal principal, UUID reportId);
}