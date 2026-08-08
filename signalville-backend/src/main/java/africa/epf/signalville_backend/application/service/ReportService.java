package africa.epf.signalville_backend.application.service;

import africa.epf.signalville_backend.api.dto.request.ReportFormRequest;
import africa.epf.signalville_backend.api.dto.response.PageResponse;
import africa.epf.signalville_backend.api.dto.response.ReportDetailResponse;
import africa.epf.signalville_backend.api.dto.response.ReportResponse;
import africa.epf.signalville_backend.application.mapper.ReportMapper;
import africa.epf.signalville_backend.domain.exception.BusinessRuleException;
import africa.epf.signalville_backend.domain.exception.InvalidStateTransitionException;
import africa.epf.signalville_backend.domain.exception.ResourceNotFoundException;
import africa.epf.signalville_backend.domain.model.Category;
import africa.epf.signalville_backend.domain.model.Priority;
import africa.epf.signalville_backend.domain.model.Report;
import africa.epf.signalville_backend.domain.model.ReportPhoto;
import africa.epf.signalville_backend.domain.model.ReportStatus;
import africa.epf.signalville_backend.domain.model.Role;
import africa.epf.signalville_backend.domain.model.StatusHistory;
import africa.epf.signalville_backend.domain.model.User;
import africa.epf.signalville_backend.infrastructure.persistence.CategoryRepository;
import africa.epf.signalville_backend.infrastructure.persistence.ReportPhotoRepository;
import africa.epf.signalville_backend.infrastructure.persistence.ReportReferenceGenerator;
import africa.epf.signalville_backend.infrastructure.persistence.ReportRepository;
import africa.epf.signalville_backend.infrastructure.persistence.StatusHistoryRepository;
import africa.epf.signalville_backend.infrastructure.persistence.UserRepository;
import africa.epf.signalville_backend.infrastructure.security.AppUserPrincipal;
import africa.epf.signalville_backend.infrastructure.storage.PhotoStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cas d'usage du Module 1 cote citoyen.
 *
 * Regle d'acces transverse : un CITOYEN ne voit et ne manipule que ses propres
 * signalements ; AGENT, SUPERVISEUR et ADMINISTRATEUR voient l'ensemble.
 * Le controle est fait ici, pas seulement dans les annotations de controller.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private static final int MIN_PHOTOS = 1;
    private static final int MAX_PHOTOS = 3;

    private final ReportRepository reportRepository;
    private final ReportPhotoRepository reportPhotoRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ReportReferenceGenerator referenceGenerator;
    private final PhotoStorageService photoStorage;

    @Transactional
    public ReportResponse create(AppUserPrincipal principal, ReportFormRequest form, List<MultipartFile> photos) {
        validatePhotoCount(photos);
        photos.forEach(photoStorage::validate);

        Category category = requireActiveCategory(form.categoryId());
        User citizen = userRepository.getReferenceById(principal.id());

        Report report = Report.builder()
                .reference(referenceGenerator.nextReference())
                .title(form.title().trim())
                .description(form.description().trim())
                .status(ReportStatus.NOUVEAU)
                // La priorite initiale vient de la categorie ; un superviseur pourra
                // l'ajuster au Module 2.
                .priority(category.getDefaultPriority())
                .category(category)
                .citizen(citizen)
                .latitude(form.latitude())
                .longitude(form.longitude())
                .address(form.address().trim())
                .district(blankToNull(form.district()))
                .municipality(blankToNull(form.municipality()))
                .photos(new ArrayList<>())
                .build();

        Report saved = reportRepository.saveAndFlush(report);
        attachPhotos(saved, photos);

        statusHistoryRepository.save(StatusHistory.builder()
                .report(saved)
                .previousStatus(null)
                .newStatus(ReportStatus.NOUVEAU)
                .comment("Signalement cree par le citoyen")
                .actor(citizen)
                .build());

        log.info("Signalement {} cree par {}", saved.getReference(), principal.email());
        return ReportMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> list(AppUserPrincipal principal,
                                             ReportStatus status,
                                             UUID categoryId,
                                             Priority priority,
                                             String search,
                                             Pageable pageable) {
        String normalizedSearch = blankToNull(search);
        Page<Report> page = principal.role() == Role.CITOYEN
                ? reportRepository.searchForCitizen(principal.id(), status, categoryId, priority, normalizedSearch, pageable)
                : reportRepository.searchAll(status, categoryId, priority, normalizedSearch, pageable);

        return PageResponse.from(page, ReportMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ReportDetailResponse getDetail(AppUserPrincipal principal, UUID reportId) {
        Report report = requireVisibleReport(principal, reportId);
        List<StatusHistory> history = statusHistoryRepository.findByReportIdOrderByChangedAtAsc(reportId);
        return ReportMapper.toDetailResponse(report, history);
    }

    @Transactional
    public ReportResponse update(AppUserPrincipal principal, UUID reportId,
                                 ReportFormRequest form, List<MultipartFile> photos) {
        Report report = requireOwnedReport(principal, reportId);
        requireStatus(report, ReportStatus.NOUVEAU, "Seul un signalement au statut NOUVEAU peut etre modifie");

        validatePhotoCount(photos);
        photos.forEach(photoStorage::validate);

        Category category = requireActiveCategory(form.categoryId());
        report.setTitle(form.title().trim());
        report.setDescription(form.description().trim());
        report.setCategory(category);
        report.setPriority(category.getDefaultPriority());
        report.setLatitude(form.latitude());
        report.setLongitude(form.longitude());
        report.setAddress(form.address().trim());
        report.setDistrict(blankToNull(form.district()));
        report.setMunicipality(blankToNull(form.municipality()));

        // Le contrat impose de renvoyer le jeu de photos complet : on remplace.
        List<String> obsoletePaths = report.getPhotos().stream().map(ReportPhoto::getStoragePath).toList();
        report.getPhotos().clear();
        reportRepository.saveAndFlush(report);
        attachPhotos(report, photos);
        obsoletePaths.forEach(photoStorage::deleteQuietly);

        return ReportMapper.toResponse(report);
    }

    @Transactional
    public ReportResponse cancel(AppUserPrincipal principal, UUID reportId, String reason) {
        Report report = requireOwnedReport(principal, reportId);
        requireStatus(report, ReportStatus.NOUVEAU, "Seul un signalement au statut NOUVEAU peut etre annule");

        ReportStatus previous = report.getStatus();
        report.setStatus(ReportStatus.ANNULE);
        reportRepository.save(report);

        statusHistoryRepository.save(StatusHistory.builder()
                .report(report)
                .previousStatus(previous)
                .newStatus(ReportStatus.ANNULE)
                .comment(reason)
                .actor(userRepository.getReferenceById(principal.id()))
                .build());

        return ReportMapper.toResponse(report);
    }

    /** Charge une photo apres controle de propriete, pour l'endpoint GET /photos/{id}. */
    @Transactional(readOnly = true)
    public ReportPhoto requireVisiblePhoto(AppUserPrincipal principal, UUID photoId) {
        ReportPhoto photo = reportPhotoRepository.findById(photoId)
                .orElseThrow(() -> ResourceNotFoundException.of("Photo", photoId));
        requireVisibility(principal, photo.getReport());
        return photo;
    }

    // --- helpers ---

    private void attachPhotos(Report report, List<MultipartFile> photos) {
        int order = 1;
        for (MultipartFile file : photos) {
            String path = photoStorage.store(report.getId(), file);
            report.addPhoto(ReportPhoto.builder()
                    .storagePath(path)
                    .originalName(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .sizeBytes(file.getSize())
                    .displayOrder(order++)
                    .build());
        }
        reportRepository.saveAndFlush(report);
    }

    private void validatePhotoCount(List<MultipartFile> photos) {
        int count = photos == null ? 0 : (int) photos.stream().filter(f -> f != null && !f.isEmpty()).count();
        if (count < MIN_PHOTOS) {
            throw new BusinessRuleException("Au moins une photo est obligatoire");
        }
        if (count > MAX_PHOTOS) {
            throw new BusinessRuleException("Trois photos au maximum");
        }
    }

    private Category requireActiveCategory(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> ResourceNotFoundException.of("Categorie", categoryId));
        if (!category.isActive()) {
            throw new BusinessRuleException("Categorie inactive : " + category.getName());
        }
        return category;
    }

    private Report requireVisibleReport(AppUserPrincipal principal, UUID reportId) {
        Report report = reportRepository.findDetailById(reportId)
                .orElseThrow(() -> ResourceNotFoundException.of("Signalement", reportId));
        requireVisibility(principal, report);
        return report;
    }

    private Report requireOwnedReport(AppUserPrincipal principal, UUID reportId) {
        Report report = reportRepository.findDetailById(reportId)
                .orElseThrow(() -> ResourceNotFoundException.of("Signalement", reportId));
        if (!report.isOwnedBy(principal.id())) {
            throw new AccessDeniedException("Ce signalement n'appartient pas a l'utilisateur courant");
        }
        return report;
    }

    private void requireVisibility(AppUserPrincipal principal, Report report) {
        if (principal.role() == Role.CITOYEN && !report.isOwnedBy(principal.id())) {
            throw new AccessDeniedException("Ce signalement n'appartient pas a l'utilisateur courant");
        }
    }

    private void requireStatus(Report report, ReportStatus expected, String message) {
        if (report.getStatus() != expected) {
            throw new InvalidStateTransitionException(
                    "%s (statut courant : %s)".formatted(message, report.getStatus()));
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
