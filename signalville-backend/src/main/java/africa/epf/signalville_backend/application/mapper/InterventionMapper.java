package africa.epf.signalville_backend.application.mapper;

import africa.epf.signalville_backend.api.dto.response.InterventionResponse;
import africa.epf.signalville_backend.domain.model.Intervention;
import africa.epf.signalville_backend.domain.model.InterventionProof;

import java.util.Comparator;

public final class InterventionMapper {

    private InterventionMapper() {
    }

    public static InterventionResponse toResponse(Intervention intervention) {
        return new InterventionResponse(
                intervention.getId(),
                intervention.getReport().getId(),
                UserMapper.toResponse(intervention.getAgent()),
                intervention.getStatus(),
                intervention.getAssignedAt(),
                intervention.getStartedAt(),
                intervention.getResolvedAt(),
                intervention.getInstruction(),
                intervention.getResolutionComment(),
                intervention.getProofs().stream()
                        .sorted(Comparator.comparing(InterventionProof::getDisplayOrder))
                        .map(InterventionProofMapper::toResponse)
                        .toList());
    }
}