package com.vivu.booking.mapper;

import com.vivu.booking.dto.request.HostProfileRequest;
import com.vivu.booking.dto.response.HostProfileResponse;
import com.vivu.booking.entity.HostProfile;
import com.vivu.booking.entity.User;
import com.vivu.booking.enums.HostStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HostProfileMapper {

    public static HostProfile toEntity(
            HostProfileRequest request,
            User user
    ) {
        return HostProfile.builder()
                .user(user)
                .displayName(request.getDisplayName())
                .businessName(request.getBusinessName())
                .bio(request.getBio())
                .hostStatus(HostStatus.PENDING)
                .autoBookingDefault(
                        request.getAutoBookingDefault() != null
                                ? request.getAutoBookingDefault()
                                : false
                )
                .active(true)
                .build();
    }

    public static HostProfileResponse toResponse(
            HostProfile hostProfile
    ) {
        return HostProfileResponse.builder()
                .id(hostProfile.getId())
                .username(hostProfile.getUser().getUsername())
                .displayName(hostProfile.getDisplayName())
                .businessName(hostProfile.getBusinessName())
                .bio(hostProfile.getBio())
                .hostStatus(hostProfile.getHostStatus())
                .autoBookingDefault(hostProfile.getAutoBookingDefault())
                .active(hostProfile.getActive())
                .build();
    }
}