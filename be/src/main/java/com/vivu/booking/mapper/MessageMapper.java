package com.vivu.booking.mapper;

import com.vivu.booking.dto.request.MessageSendRequest;
import com.vivu.booking.dto.response.MessageResponse;
import com.vivu.booking.entity.Message;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageMapper {

    public static Message toEntity(MessageSendRequest req) {
        return Message.builder()
                .msgType(req.getMsgType())
                .content(req.getContent())
                .build();
    }

    public static MessageResponse toResponse(Message e) {
        return MessageResponse.builder()
                .id(e.getId())
                .conversationId(e.getConversation() != null ? e.getConversation().getId() : null)
                .sender(e.getSender())
                .msgType(e.getMsgType())
                .content(e.getContent())
                .readAt(e.getReadAt())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
