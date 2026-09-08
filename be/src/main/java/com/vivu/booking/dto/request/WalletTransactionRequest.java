package com.vivu.booking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransactionRequest {
    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
    private BigDecimal amount;
    @Size(max = 50, message = "Reference type không được vượt quá 50 ký tự")
    private String referenceType;
    private Long referenceId;
}