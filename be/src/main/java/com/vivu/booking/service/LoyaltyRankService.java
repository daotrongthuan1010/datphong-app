package com.vivu.booking.service;

import com.vivu.booking.common.PageResponse;
import com.vivu.booking.dto.response.LoyaltyProfileResponse;
import com.vivu.booking.dto.response.LoyaltyRankResponse;
import com.vivu.booking.dto.response.PointHistoryResponse;
import com.vivu.booking.enums.RankNameType;

import java.util.List;

public interface LoyaltyRankService {

    List<LoyaltyRankResponse> listAll();

    PageResponse<LoyaltyRankResponse> list(int page, int size);

    LoyaltyRankResponse getByName(RankNameType name);

    LoyaltyProfileResponse getMyProfile(Long userId);

    PageResponse<PointHistoryResponse> getPointHistory(Long userId, int page, int size);
}
