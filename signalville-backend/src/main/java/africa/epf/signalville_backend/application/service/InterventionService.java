package africa.epf.signalville_backend.application.service;

import africa.epf.signalville_backend.domain.model.Intervention;
import africa.epf.signalville_backend.domain.model.InterventionProof;
import africa.epf.signalville_backend.domain.model.Report;
import africa.epf.signalville_backend.infrastructure.security.AppUserPrincipal;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Contrat du cycle de vie d'une intervention terrain (Module 2), depuis
 * l'affectation par un superviseur jusqu'a la resolution par un agent.
 * L'implementation vit dans application.service.impl.InterventionServiceImpl.
 */
public interface InterventionService {

    /**
     * Affecte un signalement a un agent. Possible uniquement depuis NOUVEAU
     * ou REOUVERT. Sur un REOUVERT, l'agent ne peut pas etre celui de la
     * derniere intervention (regle metier Seance 4).
     */
    Intervention assign(AppUserPrincipal principal, UUID reportId, UUID agentId, String instruction);

    /**
     * Reaffecte un signalement en cours de traitement a un autre agent.
     * Cloture l'intervention active (statut terminal REAFFECTEE) et en cree
     * une nouvelle.
     */
    Intervention reassign(AppUserPrincipal principal, UUID reportId, UUID newAgentId, String reason);

    /** L'agent affecte demarre le traitement sur le terrain. */
    Intervention start(AppUserPrincipal principal, UUID reportId);

    /**
     * L'agent affecte declare l'incident resolu. Commentaire et 1 a 3 preuves
     * photo sont obligatoires.
     */
    Intervention resolve(AppUserPrincipal principal, UUID reportId,
                          String resolutionComment, List<MultipartFile> proofs);

    /** Le superviseur verifie la resolution et cloture le signalement. */
    Report close(AppUserPrincipal principal, UUID reportId, String publicComment);

    /**
     * Intervention active d'un signalement, pour un affichage frontend qui
     * n'a besoin que de la donnee courante.
     */
    Intervention getActiveInterventionForReport(AppUserPrincipal principal, UUID reportId);

    /**
     * Le superviseur rouvre un signalement cloture. Une nouvelle affectation
     * (assign) est necessaire pour reprendre le traitement.
     */
    Report reopen(AppUserPrincipal principal, UUID reportId, String reason);

    /** Preuves d'une intervention, dans l'ordre d'affichage, apres controle de visibilite. */
    List<InterventionProof> listVisibleProofs(AppUserPrincipal principal, UUID interventionId);

    /**
     * Charge une preuve precise, en verifiant a la fois la visibilite de
     * l'intervention et l'appartenance reelle de la preuve a cette intervention.
     */
    InterventionProof requireVisibleProof(AppUserPrincipal principal, UUID interventionId, UUID proofId);
}