package com.vivu.booking.dto.request;

import com.vivu.booking.enums.HostStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HostProfileRequest {
    @NotBlank(message = "Display name không được để trống")
    @Size(max = 150, message = "Display name tối đa 150 ký tự")
    private String displayName;
    @Size(max = 200, message = "Business name tối đa 200 ký tự")
    private String businessName;
    @Size(max = 1000, message = "Bio tối đa 1000 ký tự")
    private String bio;
    private Boolean autoBookingDefault;
    private Boolean active;
    private HostStatus hostStatus;
}