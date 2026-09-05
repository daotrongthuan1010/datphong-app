package com.vivu.booking.service.impl;

import com.vivu.booking.dao.RoleDao;
import com.vivu.booking.dto.request.RoleCreateRequest;
import com.vivu.booking.dto.request.RoleUpdateRequest;
import com.vivu.booking.dto.response.RoleResponse;
import com.vivu.booking.entity.Role;
import com.vivu.booking.exception.BusinessException;
import com.vivu.booking.exception.ResourceNotFoundException;
import com.vivu.booking.mapper.RoleMapper;
import com.vivu.booking.service.RoleService;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RoleServiceImpl implements RoleService {

    private static final Set<String> PROTECTED_ROLE_CODES = Set.of("user", "host", "admin");

    private final RoleDao roleDao;

    public RoleServiceImpl(RoleDao roleDao) {
        this.roleDao = roleDao;
    }

    public RoleServiceImpl() {
        this(new RoleDao());
    }

    @Override
    public List<RoleResponse> list(int page, int size) {
        return roleDao.findAll(page, size).stream()
                .map(RoleMapper::toResponse)
                .toList();
    }

    @Override
    public RoleResponse getById(Long id) {
        Role role = roleDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy role id=" + id));
        return RoleMapper.toResponse(role);
    }

    @Override
    public RoleResponse create(RoleCreateRequest req) {
        String normalizedCode = req.getCode().trim().toLowerCase(Locale.ROOT);
        if (roleDao.existsByCode(normalizedCode)) {
            throw new BusinessException(409, "Code role '" + normalizedCode + "' đã tồn tại");
        }
        req.setCode(normalizedCode);
        req.setName(req.getName().trim());
        if (req.getDescription() != null) req.setDescription(req.getDescription().trim());
        Role role = RoleMapper.toEntity(req);
        roleDao.save(role);
        return RoleMapper.toResponse(role);
    }

    @Override
    public RoleResponse update(Long id, RoleUpdateRequest req) {
        Role role = roleDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy role id=" + id));
        if (req.getName() != null) {
            String name = req.getName().trim();
            if (name.isEmpty()) {
                throw new BusinessException(400, "name không được để trống");
            }
            role.setName(name);
        }
        if (req.getDescription() != null) {
            role.setDescription(req.getDescription().trim());
        }

        Role updated = roleDao.update(role);
        return RoleMapper.toResponse(updated);
    }

    @Override
    public void delete(Long id) {
        Role role = roleDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy role id=" + id));

        String normalizedCode = role.getCode() == null
                ? ""
                : role.getCode().trim().toLowerCase(Locale.ROOT);
        if (PROTECTED_ROLE_CODES.contains(normalizedCode)) {
            throw new BusinessException(403, "Không thể xoá role hệ thống '" + role.getCode() + "'");
        }

        long usersCount = roleDao.countUsersWithRole(id);
        if (usersCount > 0) {
            throw new BusinessException(409,
                    "Không thể xoá: còn " + usersCount + " user đang được gán role này. Gỡ role khỏi user trước.");
        }

        try {
            roleDao.deleteById(id);
        } catch (RuntimeException e) {
            // Phòng trường hợp role_permissions chưa có ON DELETE CASCADE, để lỗi FK
            // trả về thông báo dễ hiểu thay vì 500 "Internal server error" trần trụi.
            throw new BusinessException(409,
                    "Không thể xoá role: role đang được gán quyền (role_permissions). Gỡ quyền khỏi role trước.");
        }
    }
}
