package com.vivu.booking.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AppConstants {

    public static final String API_PREFIX = "/api";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
}
