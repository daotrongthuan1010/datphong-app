package com.vivu.booking.service;

import com.vivu.booking.dto.request.UsersResquest;
import com.vivu.booking.dto.response.UsersLoginResponse;
import com.vivu.booking.dto.response.UsersResponse;
import jakarta.servlet.http.Part;

import java.util.List;

public interface UserService {
       UsersLoginResponse login(String username, String password);
       UsersResponse getById(Long id);
       UsersResponse create(UsersResquest request, Part filePart);
       void deleteById(Long id);
       List<UsersResponse> getAll();
       UsersResponse update(Long id, UsersResquest req);

}
