package com.vivu.booking.dto.response;

import com.vivu.booking.enums.RoomStatus;
import com.vivu.booking.enums.RoomType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {
    private Long id;
    private String code;
    private String name;
    private RoomType type;
    private RoomStatus status;
    private Integer capacity;
    private Long pricePerNight;
    private String description;
    /** Ảnh bìa — URL MinIO của ảnh IMAGE đầu tiên (FE dùng cho card + hero). */
    private String imageUrl;
    /** Địa chỉ phòng (nullable — FE ẩn dòng địa chỉ khi null, không hardcode "Quận 1..."). */
    private String address;
    /** Ảnh chi tiết (room_images trên MinIO, mediaType=IMAGE) — FE render carousel prev/next. */
    private List<String> images;
    /** Video giới thiệu phòng (room_images trên MinIO, mediaType=VIDEO). */
    private List<String> videos;
    /** Toàn bộ media (ảnh + video) kèm id — dùng cho màn quản lý để xoá từng item. */
    private List<RoomMediaItem> media;
    /** Tiện nghi thật của phòng — join room_amenities -> amenities (rỗng = chưa cập nhật). */
    private List<AmenityResponse> amenities;
    /** Điểm trung bình từ reviews VISIBLE (null = chưa có đánh giá) — thay số giả ở card. */
    private Double avgRating;
    /** Số đánh giá VISIBLE của phòng. */
    private Long reviewCount;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
