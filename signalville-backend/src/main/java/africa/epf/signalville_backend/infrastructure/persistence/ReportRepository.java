package africa.epf.signalville_backend.infrastructure.persistence;

import africa.epf.signalville_backend.domain.model.Priority;
import africa.epf.signalville_backend.domain.model.Report;
import africa.epf.signalville_backend.domain.model.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    @Query("select r from Report r join fetch r.category left join fetch r.photos where r.id = :id")
    Optional<Report> findDetailById(@Param("id") UUID id);

    /**
     * Recherche paginee restreinte a un citoyen. Les filtres optionnels sont
     * neutralises lorsqu'ils valent null, ce qui evite une Specification pour un besoin simple.
     */
    @Query("""
            select r from Report r
            where r.citizen.id = :citizenId
              and (:status     is null or r.status   = :status)
              and (:categoryId is null or r.category.id = :categoryId)
              and (:priority   is null or r.priority = :priority)
              and (cast(:search as string) is null
                   or lower(r.title)     like lower(concat('%', cast(:search as string), '%'))
                   or lower(r.reference) like lower(concat('%', cast(:search as string), '%')))
            """)
    Page<Report> searchForCitizen(@Param("citizenId") UUID citizenId,
                                  @Param("status") ReportStatus status,
                                  @Param("categoryId") UUID categoryId,
                                  @Param("priority") Priority priority,
                                  @Param("search") String search,
                                  Pageable pageable);

    /** Vue elargie (agent, superviseur, administrateur) : pas de restriction de propriete. */
    @Query("""
            select r from Report r
            where (:status     is null or r.status   = :status)
              and (:categoryId is null or r.category.id = :categoryId)
              and (:priority   is null or r.priority = :priority)
              and (cast(:search as string) is null
                   or lower(r.title)     like lower(concat('%', cast(:search as string), '%'))
                   or lower(r.reference) like lower(concat('%', cast(:search as string), '%')))
            """)
    Page<Report> searchAll(@Param("status") ReportStatus status,
                           @Param("categoryId") UUID categoryId,
                           @Param("priority") Priority priority,
                           @Param("search") String search,
                           Pageable pageable);
}
