package com.vivu.booking.service.impl;

import com.vivu.booking.common.PageResponse;
import com.vivu.booking.dao.LoyaltyRankDao;
import com.vivu.booking.dao.PointHistoryDao;
import com.vivu.booking.dto.response.LoyaltyProfileResponse;
import com.vivu.booking.dto.response.LoyaltyRankResponse;
import com.vivu.booking.dto.response.PointHistoryResponse;
import com.vivu.booking.entity.LoyaltyRank;
import com.vivu.booking.enums.RankNameType;
import com.vivu.booking.exception.ResourceNotFoundException;
import com.vivu.booking.mapper.LoyaltyMapper;
import com.vivu.booking.mapper.PointHistoryMapper;
import com.vivu.booking.service.LoyaltyRankService;

import java.util.List;

public class LoyaltyRankServiceImp implements LoyaltyRankService {

    private final LoyaltyRankDao loyaltyRankDao;
    private final PointHistoryDao pointHistoryDao;

    public LoyaltyRankServiceImp(LoyaltyRankDao loyaltyRankDao, PointHistoryDao pointHistoryDao) {
        this.loyaltyRankDao = loyaltyRankDao;
        this.pointHistoryDao = pointHistoryDao;
    }

    public LoyaltyRankServiceImp() {
        this(new LoyaltyRankDao(), new PointHistoryDao());
    }

    @Override
    public List<LoyaltyRankResponse> listAll() {
        return loyaltyRankDao.findAllOrdered().stream().map(LoyaltyMapper::toResponse).toList();
    }

    @Override
    public PageResponse<LoyaltyRankResponse> list(int page, int size) {
        List<LoyaltyRankResponse> all = listAll();
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        List<LoyaltyRankResponse> content = all.subList(from, to);
        return PageResponse.of(content, page, size, all.size());
    }

    @Override
    public LoyaltyRankResponse getByName(RankNameType name) {
        LoyaltyRank rank = loyaltyRankDao.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hạng: " + name));
        return LoyaltyMapper.toResponse(rank);
    }

    @Override
    public LoyaltyProfileResponse getMyProfile(Long userId) {
        long totalPoints = pointHistoryDao.sumPointsByUserId(userId);
        int total = (int) Math.max(0, totalPoints);
        LoyaltyRank current = loyaltyRankDao.findHighestRankForPoints(total).orElse(null);
        LoyaltyRank next = loyaltyRankDao.findNextRank(total).orElse(null);
        Integer pointsToNext = null;
        if (next != null) {
            pointsToNext = next.getMinPoints() - total;
        }
        return LoyaltyProfileResponse.builder()
                .totalPoints(totalPoints)
                .currentRank(current != null ? LoyaltyMapper.toResponse(current) : null)
                .nextRank(next != null ? LoyaltyMapper.toResponse(next) : null)
                .pointsToNextRank(pointsToNext)
                .build();
    }

    @Override
    public PageResponse<PointHistoryResponse> getPointHistory(Long userId, int page, int size) {
        long total = pointHistoryDao.countByUserId(userId);
        List<PointHistoryResponse> content = pointHistoryDao.findByUserId(userId, page, size)
                .stream().map(PointHistoryMapper::toResponse).toList();
        return PageResponse.of(content, page, size, total);
    }
}
