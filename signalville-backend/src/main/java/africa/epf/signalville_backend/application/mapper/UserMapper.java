package africa.epf.signalville_backend.application.mapper;

import africa.epf.signalville_backend.api.dto.response.UserResponse;
import africa.epf.signalville_backend.domain.model.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getLastLoginAt(),
                user.isMustChangePassword());
    }
}