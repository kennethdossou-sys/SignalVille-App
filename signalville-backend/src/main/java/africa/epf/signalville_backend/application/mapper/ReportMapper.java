package africa.epf.signalville_backend.application.mapper;

import africa.epf.signalville_backend.api.dto.response.HistoryResponse;
import africa.epf.signalville_backend.api.dto.response.PhotoResponse;
import africa.epf.signalville_backend.api.dto.response.ReportDetailResponse;
import africa.epf.signalville_backend.api.dto.response.ReportResponse;
import africa.epf.signalville_backend.domain.model.Report;
import africa.epf.signalville_backend.domain.model.ReportPhoto;
import africa.epf.signalville_backend.domain.model.StatusHistory;

import java.util.Comparator;
import java.util.List;

public final class ReportMapper {

    /**
     * Les binaires ne sont jamais exposes en chemin disque : le client passe par
     * l'endpoint authentifie /photos/{id}. Le prefixe /api/v1 vient du context-path.
     */
    private static final String PHOTO_URL_TEMPLATE = "/api/v1/photos/%s";

    private ReportMapper() {
    }

    public static ReportResponse toResponse(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getReference(),
                report.getTitle(),
                report.getDescription(),
                report.getStatus(),
                report.getPriority(),
                CategoryMapper.toResponse(report.getCategory()),
                report.getLatitude(),
                report.getLongitude(),
                report.getAddress(),
                report.getDistrict(),
                report.getMunicipality(),
                toPhotoResponses(report),
                report.getCreatedAt(),
                report.getUpdatedAt());
    }

    public static ReportDetailResponse toDetailResponse(Report report, List<StatusHistory> history) {
        return new ReportDetailResponse(
                report.getId(),
                report.getReference(),
                report.getTitle(),
                report.getDescription(),
                report.getStatus(),
                report.getPriority(),
                CategoryMapper.toResponse(report.getCategory()),
                report.getLatitude(),
                report.getLongitude(),
                report.getAddress(),
                report.getDistrict(),
                report.getMunicipality(),
                toPhotoResponses(report),
                report.getCreatedAt(),
                report.getUpdatedAt(),
                UserMapper.toResponse(report.getCitizen()),
                // Module 2 (Interventions) non implemente en Seance 2.
                null,
                history.stream().map(ReportMapper::toHistoryResponse).toList());
    }

    public static PhotoResponse toPhotoResponse(ReportPhoto photo) {
        return new PhotoResponse(
                photo.getId(),
                PHOTO_URL_TEMPLATE.formatted(photo.getId()),
                photo.getDescription(),
                photo.getDisplayOrder(),
                photo.getCreatedAt());
    }

    public static HistoryResponse toHistoryResponse(StatusHistory entry) {
        return new HistoryResponse(
                entry.getId(),
                entry.getPreviousStatus(),
                entry.getNewStatus(),
                entry.getComment(),
                UserMapper.toResponse(entry.getActor()),
                entry.getChangedAt());
    }

    private static List<PhotoResponse> toPhotoResponses(Report report) {
        return report.getPhotos().stream()
                .sorted(Comparator.comparing(ReportPhoto::getDisplayOrder))
                .map(ReportMapper::toPhotoResponse)
                .toList();
    }
}
