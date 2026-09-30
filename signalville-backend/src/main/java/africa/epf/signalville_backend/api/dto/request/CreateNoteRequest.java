package africa.epf.signalville_backend.api.dto.request;

import africa.epf.signalville_backend.domain.model.NoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateNoteRequest(

        @NotBlank @Size(max = 1000)
        String content,

        @NotNull
        NoteType type
) {
}