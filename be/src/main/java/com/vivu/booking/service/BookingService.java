package com.vivu.booking.service;

import com.vivu.booking.common.PageResponse;
import com.vivu.booking.dto.request.BookingCreateRequest;
import com.vivu.booking.dto.response.BookingResponse;

import java.time.LocalDate;

public interface BookingService {
    BookingResponse create(Long userId, BookingCreateRequest req);
    PageResponse<BookingResponse> listByUser(Long userId, int page, int size);
    BookingResponse getById(Long userId, Long id);
    BookingResponse cancel(Long userId, Long id);
    /** FE RoomDetail goi truoc khi bam "Dat ngay" — kiem tra trung lich. */
    boolean isAvailable(Long roomId, LocalDate checkin, LocalDate checkout);
}
