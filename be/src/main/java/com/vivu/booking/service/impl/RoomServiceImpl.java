package com.vivu.booking.service.impl;

import com.vivu.booking.common.PageResponse;
import com.vivu.booking.dao.RoomDao;
import com.vivu.booking.dto.request.RoomCreateRequest;
import com.vivu.booking.dto.request.RoomUpdateRequest;
import com.vivu.booking.dto.response.RoomResponse;
import com.vivu.booking.entity.Room;
import com.vivu.booking.enums.RoomStatus;
import com.vivu.booking.enums.RoomType;
import com.vivu.booking.exception.BusinessException;
import com.vivu.booking.exception.ResourceNotFoundException;
import com.vivu.booking.mapper.RoomMapper;
import com.vivu.booking.service.RoomService;
import jakarta.servlet.http.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RoomServiceImpl implements RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomServiceImpl.class);
    private final RoomDao roomDao;

    public RoomServiceImpl(RoomDao roomDao) { this.roomDao = roomDao; }
    public RoomServiceImpl() { this(new RoomDao()); }

    @Override
    public RoomResponse create(RoomCreateRequest req) {
        if (roomDao.existsByCode(req.getCode())) {
            throw new BusinessException(409, "Room code already exists: " + req.getCode());
        }
        Room entity = RoomMapper.toEntity(req);
        roomDao.save(entity);
        log.info("Room created id={} code={}", entity.getId(), entity.getCode());
        return RoomMapper.toResponse(entity);
    }

    @Override
    public RoomResponse getById(Long id) {
        Room r = roomDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
        return RoomMapper.toResponse(r);
    }

    @Override
    public PageResponse<RoomResponse> list(RoomType type, RoomStatus status, String keyword, int page, int size) {
        long total = roomDao.countSearch(type, status, keyword);
        List<RoomResponse> content = roomDao.search(type, status, keyword, page, size)
                .stream().map(RoomMapper::toResponse).toList();
        return PageResponse.of(content, page, size, total);
    }

    @Override
    public RoomResponse update(Long id, RoomUpdateRequest req) {
        Room r = roomDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
        if (req.getName() != null) r.setName(req.getName());
        if (req.getType() != null) r.setType(req.getType());
        if (req.getStatus() != null) r.setStatus(req.getStatus());
        if (req.getCapacity() != null) r.setCapacity(req.getCapacity());
        if (req.getPricePerNight() != null) r.setPricePerNight(req.getPricePerNight());
        if (req.getDescription() != null) r.setDescription(req.getDescription());
        if (req.getImageUrl() != null) r.setImageUrl(req.getImageUrl());
        if (req.getActive() != null) r.setActive(req.getActive());
        Room merged = roomDao.update(r);
        return RoomMapper.toResponse(merged);
    }

    @Override
    public void delete(Long id) {
        Room r = roomDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
        r.setActive(false);
        roomDao.update(r);
    }
}
