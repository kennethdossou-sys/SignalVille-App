package africa.epf.signalville_backend.application.mapper;

import africa.epf.signalville_backend.api.dto.response.NotificationResponse;
import africa.epf.signalville_backend.domain.model.Notification;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getLink(),
                notification.getCreatedAt());
    }
}