package africa.epf.signalville_backend.application.service.impl;

import africa.epf.signalville_backend.application.service.InterventionService;
import africa.epf.signalville_backend.application.service.NotificationService;
import africa.epf.signalville_backend.domain.exception.BusinessRuleException;
import africa.epf.signalville_backend.domain.exception.InvalidStateTransitionException;
import africa.epf.signalville_backend.domain.exception.ResourceNotFoundException;
import africa.epf.signalville_backend.domain.model.AccountStatus;
import africa.epf.signalville_backend.domain.model.Intervention;
import africa.epf.signalville_backend.domain.model.InterventionProof;
import africa.epf.signalville_backend.domain.model.InterventionStatus;
import africa.epf.signalville_backend.domain.model.Report;
import africa.epf.signalville_backend.domain.model.ReportStatus;
import africa.epf.signalville_backend.domain.model.Role;
import africa.epf.signalville_backend.domain.model.StatusHistory;
import africa.epf.signalville_backend.domain.model.User;
import africa.epf.signalville_backend.infrastructure.persistence.InterventionProofRepository;
import africa.epf.signalville_backend.infrastructure.persistence.InterventionRepository;
import africa.epf.signalville_backend.infrastructure.persistence.ReportRepository;
import africa.epf.signalville_backend.infrastructure.persistence.StatusHistoryRepository;
import africa.epf.signalville_backend.infrastructure.persistence.UserRepository;
import africa.epf.signalville_backend.infrastructure.security.AppUserPrincipal;
import africa.epf.signalville_backend.infrastructure.storage.PhotoStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repartition volontaire avec ReportServiceImpl : tout ce qui cree/modifie
 * une Intervention vit ici. Le rejet d'un signalement (reject) n'implique
 * aucune intervention -> il reste dans ReportServiceImpl.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterventionServiceImpl implements InterventionService {

    private static final int MIN_PROOFS = 1;
    private static final int MAX_PROOFS = 3;

    private final InterventionRepository interventionRepository;
    private final InterventionProofRepository interventionProofRepository;
    private final ReportRepository reportRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final UserRepository userRepository;
    private final PhotoStorageService photoStorage;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public Intervention assign(AppUserPrincipal principal, UUID reportId, UUID agentId, String instruction) {
        Report report = requireReport(reportId);
        requireReportStatus(report, "Affectation impossible", ReportStatus.NOUVEAU, ReportStatus.REOUVERT);

        interventionRepository.findActiveByReportId(reportId).ifPresent(existing -> {
            throw new BusinessRuleException("Ce signalement a deja une intervention active");
        });

        User agent = requireEligibleAgent(agentId);

        if (report.getStatus() == ReportStatus.REOUVERT) {
            requireDifferentFromLastAgent(reportId, agentId);
        }

        User supervisor = userRepository.getReferenceById(principal.id());

        Intervention intervention = Intervention.builder()
                .report(report)
                .agent(agent)
                .status(InterventionStatus.AFFECTEE)
                .instruction(blankToNull(instruction))
                .build();
        interventionRepository.save(intervention);

        transitionReportStatus(report, ReportStatus.AFFECTE,
                "Affecte a " + agent.getFirstName() + " " + agent.getLastName(), supervisor);

        notificationService.notify(agent, "Nouvelle affectation",
                "Le signalement " + report.getReference() + " vous a ete affecte.",
                "/reports/" + report.getId());

        log.info("Signalement {} affecte a {} par {}", report.getReference(), agent.getEmail(), principal.email());
        return intervention;
    }

    @Override
    @Transactional
    public Intervention reassign(AppUserPrincipal principal, UUID reportId, UUID newAgentId, String reason) {
        requireNonBlank(reason, "Un motif de reaffectation est obligatoire");
        Report report = requireReport(reportId);

        Intervention current = interventionRepository.findActiveByReportId(reportId)
                .orElseThrow(() -> new InvalidStateTransitionException(
                        "Aucune intervention active a reaffecter pour ce signalement"));

        User newAgent = requireEligibleAgent(newAgentId);
        User supervisor = userRepository.getReferenceById(principal.id());

        current.setStatus(InterventionStatus.REAFFECTEE);
        interventionRepository.save(current);

        Intervention next = Intervention.builder()
                .report(report)
                .agent(newAgent)
                .status(InterventionStatus.AFFECTEE)
                .instruction(current.getInstruction())
                .build();
        interventionRepository.save(next);

        transitionReportStatus(report, ReportStatus.AFFECTE, reason, supervisor);

        notificationService.notify(newAgent, "Reaffectation",
                "Le signalement " + report.getReference() + " vous a ete reaffecte.",
                "/reports/" + report.getId());
        notificationService.notify(current.getAgent(), "Dossier reaffecte",
                "Le signalement " + report.getReference() + " a ete confie a un autre agent.",
                "/reports/" + report.getId());

        log.info("Signalement {} reaffecte de {} vers {} par {}",
                report.getReference(), current.getAgent().getEmail(), newAgent.getEmail(), principal.email());
        return next;
    }

    @Override
    @Transactional
    public Intervention start(AppUserPrincipal principal, UUID reportId) {
        Intervention intervention = requireOwnActiveIntervention(principal, reportId);
        requireInterventionStatus(intervention, InterventionStatus.AFFECTEE,
                "Seule une intervention AFFECTEE peut etre demarree");

        intervention.setStatus(InterventionStatus.EN_COURS);
        intervention.setStartedAt(LocalDateTime.now());
        interventionRepository.save(intervention);

        transitionReportStatus(intervention.getReport(), ReportStatus.EN_COURS,
                "Intervention demarree par l'agent", intervention.getAgent());

        return intervention;
    }

    @Override
    @Transactional
    public Intervention resolve(AppUserPrincipal principal, UUID reportId,
                                 String resolutionComment, List<MultipartFile> proofs) {
        Intervention intervention = requireOwnActiveIntervention(principal, reportId);
        requireInterventionStatus(intervention, InterventionStatus.EN_COURS,
                "Seule une intervention EN_COURS peut etre resolue");
        requireNonBlank(resolutionComment, "Un commentaire de resolution est obligatoire");
        validateProofCount(proofs);
        proofs.forEach(photoStorage::validate);

        intervention.setResolutionComment(resolutionComment.trim());
        intervention.setStatus(InterventionStatus.RESOLUE);
        intervention.setResolvedAt(LocalDateTime.now());
        interventionRepository.saveAndFlush(intervention);

        attachProofs(intervention, proofs);

        transitionReportStatus(intervention.getReport(), ReportStatus.RESOLU,
                "Intervention resolue, en attente de verification par le superviseur",
                intervention.getAgent());

        notificationService.notify(intervention.getReport().getCitizen(), "Signalement resolu",
                "Votre signalement " + intervention.getReport().getReference()
                        + " a ete traite et est en attente de verification.",
                "/reports/" + intervention.getReport().getId());

        log.info("Intervention {} resolue par {}", intervention.getId(), principal.email());
        return intervention;
    }

    @Override
    @Transactional
    public Report close(AppUserPrincipal principal, UUID reportId, String publicComment) {
        Report report = requireReport(reportId);
        requireReportStatus(report, "Cloture impossible", ReportStatus.RESOLU);

        User supervisor = userRepository.getReferenceById(principal.id());
        transitionReportStatus(report, ReportStatus.CLOTURE, blankToNull(publicComment), supervisor);

        notificationService.notify(report.getCitizen(), "Signalement cloture",
                "Votre signalement " + report.getReference() + " a ete cloture.",
                "/reports/" + report.getId());

        return report;
    }

    @Override
    @Transactional(readOnly = true)
    public Intervention getActiveInterventionForReport(AppUserPrincipal principal, UUID reportId) {
        Report report = requireReport(reportId);
        if (principal.role() == Role.CITOYEN && !report.isOwnedBy(principal.id())) {
            throw new AccessDeniedException("Ce signalement n'appartient pas a l'utilisateur courant");
        }
        return interventionRepository.findActiveByReportId(reportId)
                .orElseThrow(() -> ResourceNotFoundException.of("Intervention active pour ce signalement", reportId));
    }

    @Override
    @Transactional
    public Report reopen(AppUserPrincipal principal, UUID reportId, String reason) {
        requireNonBlank(reason, "Un motif de reouverture est obligatoire");
        Report report = requireReport(reportId);
        requireReportStatus(report, "Reouverture impossible", ReportStatus.CLOTURE);

        User supervisor = userRepository.getReferenceById(principal.id());
        transitionReportStatus(report, ReportStatus.REOUVERT, reason, supervisor);

        notificationService.notify(report.getCitizen(), "Signalement rouvert",
                "Votre signalement " + report.getReference() + " a ete rouvert : " + reason,
                "/reports/" + report.getId());

        return report;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterventionProof> listVisibleProofs(AppUserPrincipal principal, UUID interventionId) {
        Intervention intervention = requireVisibleIntervention(principal, interventionId);
        return interventionProofRepository.findByInterventionIdOrderByDisplayOrderAsc(intervention.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public InterventionProof requireVisibleProof(AppUserPrincipal principal, UUID interventionId, UUID proofId) {
        Intervention intervention = requireVisibleIntervention(principal, interventionId);
        InterventionProof proof = interventionProofRepository.findById(proofId)
                .orElseThrow(() -> ResourceNotFoundException.of("Preuve", proofId));
        if (!proof.getIntervention().getId().equals(intervention.getId())) {
            throw ResourceNotFoundException.of("Preuve", proofId);
        }
        return proof;
    }

    private Intervention requireVisibleIntervention(AppUserPrincipal principal, UUID interventionId) {
        Intervention intervention = interventionRepository.findById(interventionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Intervention", interventionId));
        if (principal.role() == Role.CITOYEN && !intervention.getReport().isOwnedBy(principal.id())) {
            throw new AccessDeniedException("Cette intervention n'est pas visible pour l'utilisateur courant");
        }
        return intervention;
    }

    // --- helpers ---

    private void attachProofs(Intervention intervention, List<MultipartFile> proofs) {
        int order = 1;
        for (MultipartFile file : proofs) {
            String path = photoStorage.store(intervention.getId(), file);
            InterventionProof proof = InterventionProof.builder()
                    .intervention(intervention)
                    .storagePath(path)
                    .originalName(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .sizeBytes(file.getSize())
                    .displayOrder(order++)
                    .build();
            intervention.getProofs().add(proof);
        }
        interventionRepository.saveAndFlush(intervention);
    }

    private void validateProofCount(List<MultipartFile> proofs) {
        int count = proofs == null ? 0 : (int) proofs.stream().filter(f -> f != null && !f.isEmpty()).count();
        if (count < MIN_PROOFS) {
            throw new BusinessRuleException("Au moins une preuve photo est obligatoire pour resoudre");
        }
        if (count > MAX_PROOFS) {
            throw new BusinessRuleException("Trois preuves photo au maximum");
        }
    }

    private void transitionReportStatus(Report report, ReportStatus newStatus, String comment, User actor) {
        ReportStatus previous = report.getStatus();
        report.setStatus(newStatus);
        reportRepository.save(report);

        statusHistoryRepository.save(StatusHistory.builder()
                .report(report)
                .previousStatus(previous)
                .newStatus(newStatus)
                .comment(comment)
                .actor(actor)
                .build());
    }

    private Report requireReport(UUID reportId) {
        return reportRepository.findDetailById(reportId)
                .orElseThrow(() -> ResourceNotFoundException.of("Signalement", reportId));
    }

    private void requireReportStatus(Report report, String context, ReportStatus... allowed) {
        for (ReportStatus status : allowed) {
            if (report.getStatus() == status) {
                return;
            }
        }
        throw new InvalidStateTransitionException(
                "%s (statut courant : %s)".formatted(context, report.getStatus()));
    }

    private Intervention requireOwnActiveIntervention(AppUserPrincipal principal, UUID reportId) {
        Intervention intervention = interventionRepository.findActiveByReportId(reportId)
                .orElseThrow(() -> ResourceNotFoundException.of("Intervention active", reportId));
        if (!intervention.getAgent().getId().equals(principal.id())) {
            throw new AccessDeniedException("Cette intervention n'est pas affectee a l'utilisateur courant");
        }
        return intervention;
    }

    private void requireInterventionStatus(Intervention intervention, InterventionStatus expected, String message) {
        if (intervention.getStatus() != expected) {
            throw new InvalidStateTransitionException(
                    "%s (statut courant : %s)".formatted(message, intervention.getStatus()));
        }
    }

    private User requireEligibleAgent(UUID agentId) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Agent", agentId));
        if (agent.getRole() != Role.AGENT) {
            throw new BusinessRuleException("L'utilisateur designe n'a pas le role AGENT");
        }
        if (agent.getStatus() != AccountStatus.ACTIF) {
            throw new BusinessRuleException("Cet agent n'est pas actif");
        }
        return agent;
    }

    private void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(message);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void requireDifferentFromLastAgent(UUID reportId, UUID agentId) {
        interventionRepository.findByReportIdOrderByAssignedAtDesc(reportId).stream()
                .findFirst()
                .ifPresent(lastIntervention -> {
                    if (lastIntervention.getAgent().getId().equals(agentId)) {
                        throw new BusinessRuleException(
                                "Ce signalement a ete rouvert : l'agent precedent ("
                                        + lastIntervention.getAgent().getFirstName() + " "
                                        + lastIntervention.getAgent().getLastName()
                                        + ") ne peut pas etre reaffecte sur ce dossier");
                    }
                });
    }
}