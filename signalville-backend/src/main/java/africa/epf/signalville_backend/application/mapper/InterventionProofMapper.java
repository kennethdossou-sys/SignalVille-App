package africa.epf.signalville_backend.application.mapper;

import africa.epf.signalville_backend.api.dto.response.PhotoResponse;
import africa.epf.signalville_backend.domain.model.InterventionProof;

/**
 * Reutilise le schema PhotoResponse du contrat (identique a celui de
 * ReportPhoto), mais avec un template d'URL distinct : les preuves
 * d'intervention sont servies par InterventionController, jamais par
 * PhotoController, pour garder la separation metier ReportPhoto / InterventionProof.
 */

public final class InterventionProofMapper {

    private static final String PROOF_URL_TEMPLATE = "/api/v1/interventions/%s/proofs/%s";

    private InterventionProofMapper() {
    }

    public static PhotoResponse toResponse(InterventionProof proof) {
        return new PhotoResponse(
                proof.getId(),
                PROOF_URL_TEMPLATE.formatted(proof.getIntervention().getId(), proof.getId()),
                proof.getDescription(),
                proof.getDisplayOrder(),
                proof.getCreatedAt());
    }
}