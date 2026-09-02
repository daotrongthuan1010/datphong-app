package com.vivu.booking.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationResponse {
    private Long id;

    private Long userId;
    private String userFullName;

    private Long hostId;
    private String hostFullName;

    private Long roomId;
    private String roomName;

    /** Nội dung tin nhắn cuối cùng, để hiển thị preview trong danh sách hội thoại. */
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;

    private LocalDateTime createdAt;
}
