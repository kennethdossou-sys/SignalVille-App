package africa.epf.signalville_backend.api.dto.response;

import java.util.List;

public record UserPage(
        List<UserResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}