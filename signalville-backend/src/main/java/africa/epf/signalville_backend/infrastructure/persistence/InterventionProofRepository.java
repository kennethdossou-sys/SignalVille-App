package africa.epf.signalville_backend.infrastructure.persistence;

import africa.epf.signalville_backend.domain.model.InterventionProof;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterventionProofRepository extends JpaRepository<InterventionProof, UUID> {

    /** Preuves d'une intervention, dans l'ordre d'affichage voulu (1 à 3). */
    List<InterventionProof> findByInterventionIdOrderByDisplayOrderAsc(UUID interventionId);
}