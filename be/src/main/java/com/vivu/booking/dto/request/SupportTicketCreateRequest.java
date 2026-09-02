package com.vivu.booking.dto.request;

import com.vivu.booking.enums.TicketTypeEnum;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicketCreateRequest {

    private Long bookingId;

    @NotNull(message = "Loại khiếu nại không được để trống")
    private TicketTypeEnum ticketType;

    @Size(max = 2000, message = "Mô tả không được quá 2000 ký tự")
    private String description;

    /** Người bị khiếu nại (VD: host báo khách làm hỏng đồ -> đây là userId của khách). Có thể để trống. */
    private Long againstUserId;
}
