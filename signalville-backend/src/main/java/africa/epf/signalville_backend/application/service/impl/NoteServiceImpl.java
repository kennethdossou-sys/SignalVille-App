package africa.epf.signalville_backend.application.service.impl;

import africa.epf.signalville_backend.application.service.NoteService;
import africa.epf.signalville_backend.domain.exception.BusinessRuleException;
import africa.epf.signalville_backend.domain.exception.ResourceNotFoundException;
import africa.epf.signalville_backend.domain.model.NoteType;
import africa.epf.signalville_backend.domain.model.Report;
import africa.epf.signalville_backend.domain.model.ReportNote;
import africa.epf.signalville_backend.domain.model.Role;
import africa.epf.signalville_backend.domain.model.User;
import africa.epf.signalville_backend.infrastructure.persistence.ReportNoteRepository;
import africa.epf.signalville_backend.infrastructure.persistence.ReportRepository;
import africa.epf.signalville_backend.infrastructure.persistence.UserRepository;
import africa.epf.signalville_backend.infrastructure.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final ReportNoteRepository reportNoteRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ReportNote add(AppUserPrincipal principal, UUID reportId, String content, NoteType type) {
        if (content == null || content.isBlank()) {
            throw new BusinessRuleException("Le contenu de la note est obligatoire");
        }
        Report report = requireReport(reportId);
        User author = userRepository.getReferenceById(principal.id());

        ReportNote note = ReportNote.builder()
                .report(report)
                .author(author)
                .content(content.trim())
                .type(type)
                .build();

        return reportNoteRepository.save(note);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportNote> list(AppUserPrincipal principal, UUID reportId) {
        requireReport(reportId);
        return principal.role() == Role.CITOYEN
                ? reportNoteRepository.findByReportIdAndTypeOrderByCreatedAtAsc(reportId, NoteType.PUBLIC)
                : reportNoteRepository.findByReportIdOrderByCreatedAtAsc(reportId);
    }

    private Report requireReport(UUID reportId) {
        return reportRepository.findDetailById(reportId)
                .orElseThrow(() -> ResourceNotFoundException.of("Signalement", reportId));
    }
}