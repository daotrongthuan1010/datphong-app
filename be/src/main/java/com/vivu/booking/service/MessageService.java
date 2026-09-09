package com.vivu.booking.service;

import com.vivu.booking.common.PageResponse;
import com.vivu.booking.dto.response.MessageResponse;

public interface MessageService {

    PageResponse<MessageResponse> listMessages(Long userId, Long conversationId, int page, int size);

    MessageResponse sendMessage(Long senderId, Long conversationId, String content);

    void markRead(Long readerId, Long conversationId);

    long countUnread(Long userId, Long conversationId);
}
