package com.vivu.booking.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopRoomResponse {
    private Long roomId;
    private String roomCode;
    private String roomName;
    private BigDecimal revenue;
    private long bookings;
    private long nights;
}
