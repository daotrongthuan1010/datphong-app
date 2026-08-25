package com.vivu.booking.service;

import com.vivu.booking.common.PageResponse;
import com.vivu.booking.dto.request.RoomCreateRequest;
import com.vivu.booking.dto.request.RoomUpdateRequest;
import com.vivu.booking.dto.response.RoomResponse;
import com.vivu.booking.enums.RoomStatus;
import com.vivu.booking.enums.RoomType;
import jakarta.servlet.http.Part;

public interface RoomService {
    RoomResponse create(RoomCreateRequest req);
    RoomResponse getById(Long id);
    PageResponse<RoomResponse> list(RoomType type, RoomStatus status, String keyword, int page, int size);
    RoomResponse update(Long id, RoomUpdateRequest req);
    void delete(Long id);
}
