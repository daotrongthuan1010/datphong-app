package com.vivu.booking.service;

import com.vivu.booking.common.PageResponse;
import com.vivu.booking.dto.request.UsersResquest;
import com.vivu.booking.dto.response.UsersLoginResponse;
import com.vivu.booking.dto.response.UsersResponse;
import com.vivu.booking.enums.RoomStatus;
import com.vivu.booking.enums.UserStatus;
import com.vivu.booking.enums.UserType;
import jakarta.servlet.http.Part;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface UserService {
       UsersLoginResponse login(String username, String password);
       UsersResponse getById(Long id);
       UsersResponse create(UsersResquest request, Part filePart);
       void deleteById(Long id);
       PageResponse<UsersResponse> list(UserType type, UserStatus status, String keyword, int page, int size);
       UsersResponse update(Long id, UsersResquest req,Part filePart);
       void exportExcel(OutputStream outputStream,UserType type, UserStatus status, String keyword, int page, int size);
//       void importExcel(InputStream inputStream);
}
