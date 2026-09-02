package com.vivu.booking.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HostVipPackageResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer durationDays;
    private Map<String, Object> benefits;
}
