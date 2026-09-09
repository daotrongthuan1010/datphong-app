package com.vivu.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationCreateRequest {

    @NotNull(message = "hostId không được để trống")
    private Long hostId;

    /** Liên kết với phòng (không bắt buộc — để hỏi chung). */
    private Long roomId;
}
