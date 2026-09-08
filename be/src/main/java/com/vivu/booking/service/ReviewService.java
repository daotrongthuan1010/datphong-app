package com.vivu.booking.service;

import com.vivu.booking.dto.request.ReviewCreateRequest;
import com.vivu.booking.dto.response.ReviewResponse;
import jakarta.servlet.http.Part;

import java.util.List;
import java.util.Map;

public interface ReviewService {

    /**
     * Danh sach review đang hiển thị của phòng + điểm trung bình.
     * Tra ve { content, page, size, totalElements, totalPages, avgRating }.
     */
    Map<String, Object> listByRoom(Long roomId, int page, int size);

    /**
     * Tao review: chi chu so huu booking (CONFIRMED/COMPLETED) moi duoc danh gia,
     * moi booking 1 lan. Media (anh/video) upload len MinIO.
     */
    ReviewResponse create(Long userId, ReviewCreateRequest req, List<Part> mediaParts);

    /** 50. POST /api/reviews/{id}/media - chủ review upload thêm ảnh/video vào review đã tạo trước đó. */
    ReviewResponse addMedia(Long userId, Long reviewId, List<Part> mediaParts);

    /** 51. PUT /api/reviews/{id}/hide - Admin ẩn review vi phạm (không xoá, chỉ đổi status). */
    ReviewResponse hide(Long reviewId);
}
