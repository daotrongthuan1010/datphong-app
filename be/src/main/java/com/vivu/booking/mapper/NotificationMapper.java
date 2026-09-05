package com.vivu.booking.mapper;

import com.vivu.booking.dto.response.NotificationResponse;
import com.vivu.booking.entity.Notification;
import com.vivu.booking.entity.User;
import com.vivu.booking.enums.NotificationChannelType;
import com.vivu.booking.enums.NotificationTypeEnum;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NotificationMapper {

    public static Notification create(User user, NotificationTypeEnum type, String title,
                                      String content, NotificationChannelType channel) {
        return Notification.builder()
                .user(user)
                .notifType(type)
                .title(title)
                .content(content)
                .channel(channel != null ? channel : NotificationChannelType.IN_APP)
                .isRead(false)
                .build();
    }

    public static NotificationResponse toResponse(Notification e) {
        return NotificationResponse.builder()
                .id(e.getId())
                .userId(e.getUser() != null ? e.getUser().getId() : null)
                .notifType(e.getNotifType())
                .title(e.getTitle())
                .content(e.getContent())
                .isRead(e.getIsRead())
                .channel(e.getChannel())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
