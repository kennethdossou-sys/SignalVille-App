package africa.epf.signalville_backend.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

/**
 * Genere les references SV-YYYY-NNNNNN.
 *
 * L'UPSERT ... RETURNING de PostgreSQL verrouille la ligne du compteur de
 * l'annee courante pendant la transaction, ce qui rend l'increment atomique
 * meme si plusieurs signalements sont crees simultanement.
 */
@Component
public class ReportReferenceGenerator {

    private static final String NEXT_VALUE_SQL = """
            INSERT INTO report_reference_counters (year, last_value)
            VALUES (:year, 1)
            ON CONFLICT (year) DO UPDATE
                SET last_value = report_reference_counters.last_value + 1
            RETURNING last_value
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public String nextReference() {
        int year = Year.now().getValue();
        Number nextValue = (Number) entityManager.createNativeQuery(NEXT_VALUE_SQL)
                .setParameter("year", year)
                .getSingleResult();
        return "SV-%d-%06d".formatted(year, nextValue.longValue());
    }
}
