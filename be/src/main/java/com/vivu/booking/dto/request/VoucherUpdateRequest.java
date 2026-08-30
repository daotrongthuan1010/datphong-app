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
public class VoucherUpdateRequest {


    @Size(max = 30, message = "Voucher code không được vượt quá 30 ký tự")
    private String code;


    private VoucherOwnerType type;

    private User user;

    private DiscountTypeEnum discountType;

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

    private LocalDateTime validFrom;

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