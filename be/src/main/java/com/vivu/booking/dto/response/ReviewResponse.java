package com.vivu.booking.dto.response;

import com.vivu.booking.enums.MediaTypeEnum;
import com.vivu.booking.enums.ReviewStatusType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private Long id;

    private Long bookingId;
    private String bookingCode;

    private Long userId;
    private String userFullName;

    private Long roomId;
    private String roomName;

    private Short rating;
    private String comment;
    private ReviewStatusType status;

    /** Ảnh/video đính kèm review (MinIO url). */
    private List<MediaItem> media;

    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MediaItem {
        private String url;
        private MediaTypeEnum mediaType;
    }
}
