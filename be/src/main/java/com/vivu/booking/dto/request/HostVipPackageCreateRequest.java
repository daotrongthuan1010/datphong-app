package com.vivu.booking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

/** Admin tạo gói VIP mới cho Host. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HostVipPackageCreateRequest {

    @NotBlank(message = "Tên gói không được để trống")
    private String name;

    @NotNull(message = "Giá gói không được để trống")
    @DecimalMin(value = "0.01", message = "Giá gói phải lớn hơn 0")
    private BigDecimal price;

    @NotNull(message = "Thời hạn gói không được để trống")
    @Min(value = 1, message = "Thời hạn gói phải >= 1 ngày")
    private Integer durationDays;

    private Map<String, Object> benefits;
}
