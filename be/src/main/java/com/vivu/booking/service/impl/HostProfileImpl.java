package com.vivu.booking.service.impl;

import com.vivu.booking.common.PageResponse;
import com.vivu.booking.dao.HostProfileDao;
import com.vivu.booking.dao.RoleDao;
import com.vivu.booking.dao.UsersDao;
import com.vivu.booking.dto.request.HostProfileRequest;
import com.vivu.booking.dto.response.HostProfileResponse;
import com.vivu.booking.entity.HostProfile;
import com.vivu.booking.entity.Role;
import com.vivu.booking.entity.User;
import com.vivu.booking.enums.HostStatus;
import com.vivu.booking.exception.BusinessException;
import com.vivu.booking.exception.ResourceNotFoundException;
import com.vivu.booking.mapper.HostProfileMapper;
import com.vivu.booking.service.HostProfileService;

import java.util.List;
import java.util.stream.Collectors;

public class HostProfileImpl implements HostProfileService {
    private final HostProfileDao hostProfileDao;
    private final UsersDao usersDao;
    private final RoleDao roleDao;

    public HostProfileImpl(HostProfileDao hostProfileDao, UsersDao usersDao, RoleDao roleDao) {
        this.hostProfileDao = hostProfileDao;
        this.usersDao = usersDao;
        this.roleDao = roleDao;
    }
    public HostProfileImpl(UsersDao usersDao, RoleDao roleDao) {
        this(new HostProfileDao(), usersDao, roleDao);
    }

    @Override
    public HostProfileResponse create(HostProfileRequest req,Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "Không xác định được người dùng");
        }
        User user = usersDao.findById(userId).orElseThrow(() ->
                        new ResourceNotFoundException("User không tồn tại")
                );

        if (hostProfileDao.existsByUserId(userId)) {
            throw new BusinessException(
                    409,
                    "User đã đăng ký Host"
            );
        }

        HostProfile hostProfile = HostProfileMapper.toEntity(req, user);
        hostProfileDao.save(hostProfile);
        return HostProfileMapper.toResponse(hostProfile);
    }

    @Override
    public HostProfileResponse update(Long id, HostProfileRequest req) {
        HostProfile h = hostProfileDao.findByIdWithUser(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Host profile không tồn tại: " + id
                        )
                );
        if (req.getDisplayName() != null) {
            h.setDisplayName(req.getDisplayName());
        }
        if (req.getBusinessName() != null) {
            h.setBusinessName(req.getBusinessName());
        }
        if (req.getBio() != null) {
            h.setBio(req.getBio());
        }
        if (req.getAutoBookingDefault() != null) {
            h.setAutoBookingDefault(
                    req.getAutoBookingDefault()
            );
        }
        if (req.getActive() != null) {
            h.setActive(req.getActive());
        }
        if (req.getHostStatus() != null) {
            HostStatus newStatus = req.getHostStatus();
            if (newStatus == HostStatus.APPROVED) {
                User user = h.getUser();
                Role hostRole = roleDao.findByCode("HOST")
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy role HOST"
                                )
                        );
                boolean hasHostRole = user.getRole()
                        .stream()
                        .anyMatch(role ->
                                "HOST".equalsIgnoreCase(
                                        role.getCode()
                                )
                        );
                if (!hasHostRole) {
                    user.getRole().add(hostRole);
                    usersDao.update(user);
                }
                h.setHostStatus(HostStatus.APPROVED);
            }
            else {
                h.setHostStatus(newStatus);
            }
        }
        hostProfileDao.update(h);
        HostProfile result =
                hostProfileDao.findByIdWithUser(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Host profile không tồn tại: " + id
                                )
                        );

        return HostProfileMapper.toResponse(result);
    }

    @Override
    public void delete(Long id) {
        HostProfile h = hostProfileDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Host profile not found"));
        h.setActive(false);
        hostProfileDao.update(h);
    }

    @Override
    public HostProfileResponse getById(Long id) {
        HostProfile h = hostProfileDao.findByIdWithUser(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Host profile not found: " + id
                        )
                );

        return HostProfileMapper.toResponse(h);
    }

    @Override
    public PageResponse<HostProfileResponse> list(HostStatus status, String keyword, int page, int size) {
        long total = hostProfileDao.countSearch(status, keyword);
        List<HostProfileResponse> content = hostProfileDao.search(status, keyword, page, size)
                .stream().map(HostProfileMapper::toResponse).toList();
        return PageResponse.of(content, page, size, total);
    }
}
