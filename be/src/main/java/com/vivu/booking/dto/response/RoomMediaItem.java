package com.vivu.booking.dto.response;

import lombok.*;

/** Một item media của phòng (ảnh hoặc video) — FE dùng id để xoá, mediaType để render đúng. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomMediaItem {
    private Long id;
    private String url;
    /** IMAGE hoặc VIDEO */
    private String mediaType;
}
