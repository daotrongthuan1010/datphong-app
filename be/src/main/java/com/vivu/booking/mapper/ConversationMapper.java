package com.vivu.booking.mapper;

import com.vivu.booking.dto.response.ConversationResponse;
import com.vivu.booking.entity.Conversation;
import com.vivu.booking.entity.Message;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConversationMapper {

    public static ConversationResponse toResponse(Conversation e) {
        return toResponse(e, null);
    }

    public static ConversationResponse toResponse(Conversation e, Message last) {
        return ConversationResponse.builder()
                .id(e.getId())
                .userId(e.getUser() != null ? e.getUser().getId() : null)
                .userFullName(e.getUser() != null ? e.getUser().getFullName() : null)
                .hostId(e.getHost() != null ? e.getHost().getId() : null)
                .hostFullName(e.getHost() != null ? e.getHost().getFullName() : null)
                .roomId(e.getRoom() != null ? e.getRoom().getId() : null)
                .roomName(e.getRoom() != null ? e.getRoom().getName() : null)
                .lastMessagePreview(last != null ? last.getContent() : null)
                .lastMessageAt(last != null ? last.getCreatedAt() : null)
                .createdAt(e.getCreatedAt())
                .build();
    }
}
