package com.vivu.booking.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCreateRequest {

    @NotNull(message = "Phòng cần đặt không được để trống")
    private Long roomId;

    @NotNull(message = "Ngày check-in không được để trống")
    @FutureOrPresent(message = "Ngày check-in không được ở quá khứ")
    private LocalDate checkinDate;

    @NotNull(message = "Ngày check-out không được để trống")
    private LocalDate checkoutDate;

    @NotNull(message = "Số lượng khách không được để trống")
    @Min(value = 1, message = "Phải có ít nhất 1 khách")
    private Integer guestsCount;

    /** Mã voucher áp dụng, có thể để trống nếu không dùng voucher. */
    private String voucherCode;

    @AssertTrue(message = "Ngày check-out phải sau ngày check-in")
    public boolean isValidDateRange() {
        if (checkinDate == null || checkoutDate == null) return true;
        return checkoutDate.isAfter(checkinDate);
    }
}
