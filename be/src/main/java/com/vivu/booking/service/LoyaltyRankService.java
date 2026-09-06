package com.vivu.booking.service;

import com.vivu.booking.common.PageResponse;
import com.vivu.booking.dto.response.LoyaltyRankResponse;
import com.vivu.booking.entity.LoyaltyRank;
import com.vivu.booking.enums.RankNameType;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.util.List;

public interface LoyaltyRankService {
    //find all LoyaltyRank + benefits
    PageResponse<LoyaltyRankResponse> list(RankNameType name,LoyaltyRank benefits );
    LoyaltyRankResponse getByName(RankNameType name);
}
