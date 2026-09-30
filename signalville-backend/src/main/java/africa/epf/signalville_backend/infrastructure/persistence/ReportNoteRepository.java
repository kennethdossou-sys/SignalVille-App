package africa.epf.signalville_backend.infrastructure.persistence;

import africa.epf.signalville_backend.domain.model.NoteType;
import africa.epf.signalville_backend.domain.model.ReportNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportNoteRepository extends JpaRepository<ReportNote, UUID> {

    /** Toutes les notes d'un signalement, sans filtre de visibilité (usage interne uniquement). */
    List<ReportNote> findByReportIdOrderByCreatedAtAsc(UUID reportId);

    /**
     * Notes visibles par le citoyen : uniquement celles de type PUBLIC. Le filtre
     * est fait ici, au niveau requête — pas en récupérant tout puis en filtrant en
     * Java, pour éviter qu'un oubli de filtre côté service ne fasse fuiter une note
     * interne (défense en profondeur).
     */
    List<ReportNote> findByReportIdAndTypeOrderByCreatedAtAsc(UUID reportId, NoteType type);
}