package com.vivu.booking.service;

import com.vivu.booking.common.PageResponse;
import com.vivu.booking.dto.request.VoucherCreateRequest;
import com.vivu.booking.dto.request.VoucherUpdateRequest;
import com.vivu.booking.dto.response.VoucherResponse;
import com.vivu.booking.entity.Voucher;
import com.vivu.booking.entity.VoucherUsage;
import com.vivu.booking.enums.VoucherOwnerType;

public interface VoucherService {
    VoucherResponse create(VoucherCreateRequest req);
    VoucherResponse getById(Long id);
    PageResponse<VoucherResponse> list(VoucherOwnerType type, VoucherUsage usage,String keyword,int page, int size);
    VoucherResponse update(Long id, VoucherUpdateRequest req);
    void delete(Long id);
}
