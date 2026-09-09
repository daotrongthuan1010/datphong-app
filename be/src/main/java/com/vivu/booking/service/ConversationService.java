package com.vivu.booking.service;

import com.vivu.booking.common.PageResponse;
import com.vivu.booking.dto.response.ConversationResponse;

public interface ConversationService {

    ConversationResponse getOrCreate(Long actorId, Long hostId, Long roomId);

    PageResponse<ConversationResponse> listMyConversations(Long userId, int page, int size);

    ConversationResponse getById(Long userId, Long conversationId);
}
