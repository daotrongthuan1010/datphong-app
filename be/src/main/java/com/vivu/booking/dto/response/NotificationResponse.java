package com.vivu.booking.dto.response;

import com.vivu.booking.enums.NotificationChannelType;
import com.vivu.booking.enums.NotificationTypeEnum;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private Long userId;
    private NotificationTypeEnum notifType;
    private String title;
    private String content;
    private Boolean isRead;
    private NotificationChannelType channel;
    private LocalDateTime createdAt;
}
