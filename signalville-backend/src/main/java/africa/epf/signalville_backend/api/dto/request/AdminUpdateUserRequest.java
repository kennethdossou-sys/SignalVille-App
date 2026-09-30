package africa.epf.signalville_backend.api.dto.request;

import africa.epf.signalville_backend.domain.model.Role;

public record AdminUpdateUserRequest(
        String firstName,
        String lastName,
        String phone,
        Role role
) {
}