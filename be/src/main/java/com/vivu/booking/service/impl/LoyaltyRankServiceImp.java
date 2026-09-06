package com.vivu.booking.service.impl;

import com.vivu.booking.common.PageResponse;
import com.vivu.booking.dao.LoyaltyRankDao;
import com.vivu.booking.dto.response.LoyaltyRankResponse;
import com.vivu.booking.entity.LoyaltyRank;
import com.vivu.booking.enums.RankNameType;
import com.vivu.booking.exception.ResourceNotFoundException;
import com.vivu.booking.service.LoyaltyRankService;
import com.vivu.booking.mapper.LoyaltyMapper;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.util.List;

public  class LoyaltyRankServiceImp implements LoyaltyRankService {
    private final LoyaltyRankDao loyaltyRankDao;

    public LoyaltyRankServiceImp(LoyaltyRankDao loyaltyRankDao){this.loyaltyRankDao = loyaltyRankDao;}

    public LoyaltyRankServiceImp(){this.loyaltyRankDao = new LoyaltyRankDao();}
/**Danh sách hạng + Điểm theo hạng+ quyền lợi*/

    @Override
    public PageResponse<LoyaltyRankResponse> list(RankNameType name, LoyaltyRank benefits) {
        return list(name,benefits);
    }


    @Override
    public LoyaltyRankResponse getByName(RankNameType name) {
        LoyaltyRank l = loyaltyRankDao.findByName(name).orElseThrow(() -> new ResourceNotFoundException("Rank not found: "+name));

        return null;
    }


}
