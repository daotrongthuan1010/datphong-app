package com.vivu.booking.dto.response;

import com.vivu.booking.entity.Booking;
import com.vivu.booking.entity.User;
import com.vivu.booking.enums.TicketStatusType;
import com.vivu.booking.enums.TicketTypeEnum;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicketResponse {
    private Long id;
    private Booking booking;
    private User createdBy;
    private User againstUser;
    private TicketTypeEnum ticketType;
    private String description;
    private TicketStatusType status;
    private String resolutionNote;
    private User resolvedBy;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
