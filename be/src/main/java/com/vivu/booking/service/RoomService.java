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
    /** Lọc nâng cao cho Home: thêm khoảng giá, sức chứa và sắp xếp. */
    PageResponse<RoomResponse> list(RoomType type, RoomStatus status, String keyword,
                                    Long minPrice, Long maxPrice, Integer minCapacity,
                                    int page, int size, String sortBy, String sortDir);
    RoomResponse update(Long id, RoomUpdateRequest req);
    void delete(Long id);
    /** Upload ảnh HOẶC video phòng lên MinIO + lưu vào room_images, trả URL công khai. */
    String uploadMedia(Long roomId, Part file);
    /** Xoá một item media (ảnh/video) của phòng — xoá bản ghi DB + object trên MinIO. */
    void deleteMedia(Long roomId, Long mediaId);
}
