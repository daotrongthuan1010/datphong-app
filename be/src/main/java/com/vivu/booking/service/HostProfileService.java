package com.vivu.booking.service;

import com.vivu.booking.common.PageResponse;
import com.vivu.booking.dto.request.HostProfileRequest;
import com.vivu.booking.dto.response.HostProfileResponse;
import com.vivu.booking.entity.User;
import com.vivu.booking.enums.HostStatus;

public interface HostProfileService {
       HostProfileResponse create(HostProfileRequest req, Long userId);
       HostProfileResponse update(Long id,HostProfileRequest req);
       void delete(Long id);
       HostProfileResponse getById(Long id);
       PageResponse<HostProfileResponse> list(HostStatus status,String keyword,int page,int size);
}
