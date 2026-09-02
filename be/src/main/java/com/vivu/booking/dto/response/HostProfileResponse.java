package com.vivu.booking.dto.response;

import com.vivu.booking.enums.HostStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HostProfileResponse {
       private Long id;
       private String username;
       private String displayName;
       private String businessName;
       private String bio;
       private HostStatus hostStatus;
       private Boolean autoBookingDefault;
       private Boolean active;
}
