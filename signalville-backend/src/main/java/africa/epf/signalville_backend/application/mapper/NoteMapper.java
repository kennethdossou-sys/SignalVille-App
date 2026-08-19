package africa.epf.signalville_backend.application.mapper;

import africa.epf.signalville_backend.api.dto.response.NoteResponse;
import africa.epf.signalville_backend.domain.model.ReportNote;

public final class NoteMapper {

    private NoteMapper() {
    }

    public static NoteResponse toResponse(ReportNote note) {
        return new NoteResponse(
                note.getId(),
                note.getContent(),
                note.getType(),
                UserMapper.toResponse(note.getAuthor()),
                note.getCreatedAt());
    }
}