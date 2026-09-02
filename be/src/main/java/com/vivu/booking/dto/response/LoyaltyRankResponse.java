package com.vivu.booking.dto.response;

import com.vivu.booking.enums.RankNameType;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyRankResponse {
    private Long id;
    private RankNameType name;
    private Integer minPoints;
    private BigDecimal discountPercent;
    /** VD: {"free_upgrade": true, "birthday_voucher": true} */
    private Map<String, Object> benefits;
}
