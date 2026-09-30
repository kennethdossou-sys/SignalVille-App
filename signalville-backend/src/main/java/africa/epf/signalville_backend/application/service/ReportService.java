package africa.epf.signalville_backend.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import africa.epf.signalville_backend.api.dto.request.ReportFormRequest;
import africa.epf.signalville_backend.api.dto.response.PageResponse;
import africa.epf.signalville_backend.api.dto.response.ReportDetailResponse;
import africa.epf.signalville_backend.api.dto.response.ReportResponse;
import africa.epf.signalville_backend.domain.model.Priority;
import africa.epf.signalville_backend.domain.model.ReportPhoto;
import africa.epf.signalville_backend.domain.model.ReportStatus;
import africa.epf.signalville_backend.infrastructure.security.AppUserPrincipal;

/**
 * Contrat du service de gestion des signalements (Module 1). L'implementation
 * vit dans application.service.impl.ReportServiceImpl.
 *
 * Regle d'acces transverse : un CITOYEN ne voit et ne manipule que ses propres
 * signalements ; AGENT, SUPERVISEUR et ADMINISTRATEUR voient l'ensemble.
 */
public interface ReportService {

    ReportResponse create(AppUserPrincipal principal, ReportFormRequest form, List<MultipartFile> photos);

    PageResponse<ReportResponse> list(AppUserPrincipal principal,
                                       ReportStatus status,
                                       UUID categoryId,
                                       Priority priority,
                                       String search,
                                       Pageable pageable);

    ReportDetailResponse getDetail(AppUserPrincipal principal, UUID reportId);

    ReportResponse update(AppUserPrincipal principal, UUID reportId,
                           ReportFormRequest form, List<MultipartFile> photos);

    ReportResponse cancel(AppUserPrincipal principal, UUID reportId, String reason);

    /**
     * Le superviseur rejette un signalement avant toute affectation. Possible
     * uniquement depuis NOUVEAU.
     */
    ReportResponse reject(AppUserPrincipal principal, UUID reportId, String reason);

    /** Charge une photo apres controle de propriete, pour l'endpoint GET /photos/{id}. */
    ReportPhoto requireVisiblePhoto(AppUserPrincipal principal, UUID photoId);
}