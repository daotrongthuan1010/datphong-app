package com.vivu.booking.dto.request;

import com.vivu.booking.entity.LoyaltyRank;
import com.vivu.booking.entity.User;
import com.vivu.booking.enums.DiscountTypeEnum;
import com.vivu.booking.enums.VoucherOwnerType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherCreateRequest {

    @NotBlank(message = "Voucher code không được để trống")
    @Size(max = 30, message = "Voucher code không được vượt quá 30 ký tự")
    private String code;

    @NotNull(message = "Loại chủ sở hữu voucher không được để trống")
    private VoucherOwnerType type;

    private User user;

    @NotNull(message = "Loại giảm giá không được để trống")
    private DiscountTypeEnum discountType;

    @NotNull(message = "Giá trị giảm giá không được để trống")
    @DecimalMin(
            value = "0.01",
            message = "Giá trị giảm giá phải lớn hơn 0"
    )
    private BigDecimal discountValue;

    @Min(
            value = 1,
            message = "Số đêm tối thiểu phải lớn hơn hoặc bằng 1"
    )
    private Integer minNights;

    @DecimalMin(
            value = "0.0",
            inclusive = true,
            message = "Giá trị đơn hàng tối thiểu không được âm"
    )
    private BigDecimal minOrderValue;

    private LoyaltyRank targetRank;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    private LocalDateTime validFrom;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    private LocalDateTime validTo;

    @Min(
            value = 1,
            message = "Giới hạn sử dụng tổng phải lớn hơn hoặc bằng 1"
    )
    private Integer usageLimitTotal;

    @Min(
            value = 1,
            message = "Giới hạn sử dụng mỗi user phải lớn hơn hoặc bằng 1"
    )
    private Integer usageLimitPerUser;


    @AssertTrue(message = "Thời gian bắt đầu phải trước thời gian kết thúc")
    public boolean isValidDateRange() {

        // Để @NotNull xử lý trường hợp null
        if (validFrom == null || validTo == null) {
            return true;
        }

        return validFrom.isBefore(validTo);
    }
}