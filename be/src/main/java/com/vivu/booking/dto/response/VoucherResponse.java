package com.vivu.booking.dto.response;

import com.vivu.booking.entity.LoyaltyRank;
import com.vivu.booking.entity.User;
import com.vivu.booking.enums.DiscountTypeEnum;
import com.vivu.booking.enums.VoucherOwnerType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherResponse {
    private Long id;
    private String code;
    private VoucherOwnerType ownerType;
//    private User owner;
    private DiscountTypeEnum discountType;
    private BigDecimal discountValue;
    private Integer minNights;
    private BigDecimal minOrderValue;
    private LoyaltyRank targetRank;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Integer usageLimitPerUser;
    private LocalDateTime createdAt;
}
