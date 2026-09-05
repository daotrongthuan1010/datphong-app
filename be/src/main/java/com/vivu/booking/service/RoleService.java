package com.vivu.booking.service;

import com.vivu.booking.dto.request.RoleCreateRequest;
import com.vivu.booking.dto.request.RoleUpdateRequest;
import com.vivu.booking.dto.response.RoleResponse;

import java.util.List;

public interface RoleService {

    List<RoleResponse> list(int page, int size);

    RoleResponse getById(Long id);

    RoleResponse create(RoleCreateRequest req);

    RoleResponse update(Long id, RoleUpdateRequest req);

    void delete(Long id);
}
