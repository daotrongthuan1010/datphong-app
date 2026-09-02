package com.vivu.booking.dto.request;

import com.vivu.booking.enums.DocReviewStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycReviewRequest {

    @NotNull(message = "Trạng thái duyệt không được để trống")
    private DocReviewStatus status;

    @Size(max = 500, message = "Ghi chú không được quá 500 ký tự")
    private String note;

    @AssertTrue(message = "Trạng thái duyệt phải là APPROVED hoặc REJECTED")
    public boolean isDecisiveStatus() {
        return status == null || status != DocReviewStatus.PENDING;
    }
}
