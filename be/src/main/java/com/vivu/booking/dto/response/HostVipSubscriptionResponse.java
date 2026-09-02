package com.vivu.booking.dto.response;

import com.vivu.booking.entity.User;
import com.vivu.booking.enums.VipSubStatusType;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HostVipSubscriptionResponse {
    private Long id;
    private User host;
    private HostVipPackageResponse vipPackage;
    private LocalDate startDate;
    private LocalDate endDate;
    private VipSubStatusType status;
    private String paymentRef;
}
