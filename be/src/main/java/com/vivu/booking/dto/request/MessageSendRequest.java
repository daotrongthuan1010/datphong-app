package com.vivu.booking.dto.request;

import com.vivu.booking.enums.MessageTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageSendRequest {

    @NotNull(message = "Cuộc hội thoại không được để trống")
    private Long conversationId;

    @Builder.Default
    private MessageTypeEnum msgType = MessageTypeEnum.TEXT;

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    private String content;
}
