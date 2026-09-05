package com.vivu.booking.service;

import jakarta.servlet.annotation.MultipartConfig;

import java.io.InputStream;

public interface UserImportService {
    void importExcel(InputStream inputStream) throws Exception;
}
