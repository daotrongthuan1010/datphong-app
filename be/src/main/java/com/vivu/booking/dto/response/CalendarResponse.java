package com.vivu.booking.dto.response;

import com.vivu.booking.entity.RoomCalendar;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 1 ngày trong tháng của lịch phòng — TỰ HỌC.
 *
 * <p>Dùng chung cho FE calendar (mỗi ô 1 ngày) và cho admin report (lấp đầy theo ngày).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarResponse {
    private LocalDate date;
    private String status;
    private BigDecimal priceOverride;

    public static CalendarResponse from(RoomCalendar c) {
        return CalendarResponse.builder()
                .date(c.getCalendarDate())
                .status(c.getStatus().name())
                .priceOverride(c.getPriceOverride())
                .build();
    }
}
