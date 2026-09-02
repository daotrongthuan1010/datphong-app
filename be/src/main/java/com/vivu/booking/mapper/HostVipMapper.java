package com.vivu.booking.mapper;

import com.vivu.booking.dto.request.HostVipPackageCreateRequest;
import com.vivu.booking.dto.response.HostVipPackageResponse;
import com.vivu.booking.dto.response.HostVipSubscriptionResponse;
import com.vivu.booking.entity.HostVipPackage;
import com.vivu.booking.entity.HostVipSubscription;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HostVipMapper {

    public static HostVipPackage toEntity(HostVipPackageCreateRequest req) {
        return HostVipPackage.builder()
                .name(req.getName())
                .price(req.getPrice())
                .durationDays(req.getDurationDays())
                .benefits(req.getBenefits() != null ? req.getBenefits() : Map.of())
                .build();
    }

    public static HostVipPackageResponse toResponse(HostVipPackage e) {
        return HostVipPackageResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .price(e.getPrice())
                .durationDays(e.getDurationDays())
                .benefits(e.getBenefits())
                .build();
    }

    /** host/vipPackage/startDate/endDate do Service gắn (host lấy từ JWT, package lookup theo packageId, ngày tự tính theo durationDays). */
    public static HostVipSubscriptionResponse toResponse(HostVipSubscription e) {
        return HostVipSubscriptionResponse.builder()
                .id(e.getId())
                .host(e.getHost())
                .vipPackage(e.getVipPackage() != null ? toResponse(e.getVipPackage()) : null)
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .status(e.getStatus())
                .paymentRef(e.getPaymentRef())
                .build();
    }
}
