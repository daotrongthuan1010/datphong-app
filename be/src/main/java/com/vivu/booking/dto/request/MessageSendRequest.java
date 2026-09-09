package com.vivu.booking.dto.request;

import com.vivu.booking.enums.MessageTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageSendRequest {

    /** Không bắt buộc — URL /api/conversations/{id}/messages đã mang conversationId. */
    private Long conversationId;

    @Builder.Default
    private MessageTypeEnum msgType = MessageTypeEnum.TEXT;

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    @Size(max = 4000, message = "Tin nhắn tối đa 4000 ký tự")
    private String content;
}
