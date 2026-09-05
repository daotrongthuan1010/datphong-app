package com.vivu.booking.dto.request;

import com.vivu.booking.enums.RoomStatus;
import com.vivu.booking.enums.RoomType;
import jakarta.validation.constraints.Min;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomSearchFilterRequest {

    /** Tìm theo code hoặc name (LIKE %keyword%) */
    private String keyword;

    private RoomType type;

    private RoomStatus status;

    @Min(value = 0, message = "Giá tối thiểu không được âm")
    private Long minPrice;

    @Min(value = 0, message = "Giá tối đa không được âm")
    private Long maxPrice;

    @Min(value = 1, message = "Sức chứa tối thiểu phải >= 1")
    private Integer minCapacity;

    private Boolean active;

    @Builder.Default
    @Min(value = 0, message = "Trang phải >= 0")
    private int page = 0;

    @Builder.Default
    @Min(value = 1, message = "Kích thước trang phải >= 1")
    private int size = 20;

    /** VD: "pricePerNight", "createdAt"... */
    private String sortBy;

    /** "asc" | "desc" */
    private String sortDir;
}
