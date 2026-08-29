package com.vivu.booking.service.impl;

import com.vivu.booking.common.PageResponse;
import com.vivu.booking.config.MinioConfig;
import com.vivu.booking.dao.RoleDao;
import com.vivu.booking.dao.UsersDao;
import com.vivu.booking.dto.request.UsersResquest;
import com.vivu.booking.dto.response.UsersLoginResponse;
import com.vivu.booking.dto.response.UsersResponse;
import com.vivu.booking.entity.Role;
import com.vivu.booking.entity.User;
import com.vivu.booking.enums.RoomStatus;
import com.vivu.booking.enums.UserStatus;
import com.vivu.booking.enums.UserType;
import com.vivu.booking.exception.ResourceNotFoundException;
import com.vivu.booking.mapper.UserMapper;
import com.vivu.booking.service.UserService;
import com.vivu.booking.utils.ExcelUtils;
import com.vivu.booking.utils.PasswordUntil;
import com.vivu.booking.utils.ValidationUtils;
import jakarta.persistence.*;
import jakarta.servlet.http.Part;
import lombok.Builder;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class UserServiceImpl implements UserService {
    private final UsersDao usersDao;
    private final RoleDao roleDao;
    public UserServiceImpl(UsersDao usersDao, RoleDao roleDao) {
        this.usersDao = usersDao;
        this.roleDao = roleDao;
    }

    public UserServiceImpl(RoleDao roleDao) {
        this(new UsersDao(), roleDao);
    }


    @Override
    public UsersLoginResponse login(String username, String password) {
        UsersLoginResponse usersLoginResponse = usersDao.getRolebyUsername(username);
        if(usersLoginResponse==null) {
            return null;
        }
        if(!PasswordUntil.checkPassword(password, usersLoginResponse.getPassword())) {
            return null;
        }
        return usersLoginResponse;
    }

    @Override
    public UsersResponse getById(Long id) {
        User users=usersDao.findById(id).orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
        return UserMapper.toResponse(users);
    }

    @Override
    public UsersResponse create(UsersResquest request, Part filePart) {
        Set<Long> roleIds = request.getRoleId();
        ValidationUtils.validate(request);
        if (roleIds == null || roleIds.isEmpty()) {
            throw new IllegalArgumentException("User phải có ít nhất một role");
        }

        // Kiểm tra tất cả role có tồn tại
        Set<Role> roles = new HashSet<>();

        for (Long roleId : roleIds) {
            Role role = roleDao.findById(roleId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Role không tồn tại: " + roleId
                            )
                    );

            roles.add(role);
        }

        // Tạo User
        User entity = UserMapper.toEntity(request);
        entity.setRole(roles);

        if ( ObjectUtils.isEmpty(filePart)) {
            throw new IllegalArgumentException("Chưa chọn ảnh");
        }


        try {

            String bucketName = MinioConfig.getBucket();
            MinioConfig.createBucket(bucketName);
            MinioConfig.setPublic(bucketName);
            LocalDate today = LocalDate.now();
            String folder = String.format(
                    "%d/%02d/%02d/users",
                    today.getYear(),
                    today.getMonthValue(),
                    today.getDayOfMonth()
            );

            String originalFileName = filePart.getSubmittedFileName();
            String objectName = folder
                    + "/users"
                    + UUID.randomUUID()
                    + "_"
                    + originalFileName;
            try (InputStream inputStream = filePart.getInputStream()) {
                MinioConfig.upload(
                        bucketName,
                        objectName,
                        inputStream,
                        filePart.getSize(),
                        filePart.getContentType()
                );
            }

            String imageUrl = MinioConfig.getObjectUrl(bucketName, objectName);
            entity.setAvatar(imageUrl);
            entity.setPassword(PasswordUntil.hashedPassword(entity.getPassword()));
            usersDao.save(entity);
            return UserMapper.toResponse(entity);
        } catch (Exception e) {

            throw new RuntimeException(
                    "Upload avatar thất bại",
                    e
            );
        }
    }

    @Override
    public void deleteById(Long id) {
        User users=usersDao.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("User not found: " + id));
        users.setActive(false);
        usersDao.update(users);
    }

    @Override
    public PageResponse<UsersResponse> list(UserType type, UserStatus status, String keyword, int page, int size) {
        long total=usersDao.countSearch(type,status,keyword);
        List<UsersResponse> content=usersDao.search(type,status,keyword,page,size)
                .stream().map(UserMapper::toResponse).toList();
        return PageResponse.of(content,page,size,total);

    }


    @Override
    public UsersResponse update(Long id, UsersResquest req,Part filePart) {
        User users=usersDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
        if(req.getFullName()!=null) req.setFullName(users.getFullName());
        if(req.getEmail()!=null) req.setEmail(users.getEmail());
        if(req.getPhone()!=null) req.setPhone(users.getPhone());
        if(req.getUsername()!=null) req.setUsername(users.getUsername());
        if(req.getGender()!=null) req.setGender(users.getGender());
        if (req.getAvatar() != null) users.setAvatar(req.getAvatar());
        if (req.getStatus() != null) users.setStatus(req.getStatus());
        if (req.getActive() != null) users.setActive(req.getActive());
        if (filePart != null && filePart.getSize() > 0) {

            try {

                String bucketName = MinioConfig.getBucket();

                // Tạo bucket nếu chưa có
                MinioConfig.createBucket(bucketName);

                // Set public nếu cần
                MinioConfig.setPublic(bucketName);

                // Tạo folder theo ngày
                LocalDate today = LocalDate.now();

                String folder = String.format(
                        "%d/%02d/%02d/users",
                        today.getYear(),
                        today.getMonthValue(),
                        today.getDayOfMonth()
                );

                // Tên file gốc
                String originalFileName = filePart.getSubmittedFileName();

                // Tạo tên file unique
                String objectName =
                        folder
                                + "/"
                                + UUID.randomUUID()
                                + "_"
                                + originalFileName;

                // Upload MinIO
                try (InputStream inputStream =
                             filePart.getInputStream()) {

                    MinioConfig.upload(
                            bucketName,
                            objectName,
                            inputStream,
                            filePart.getSize(),
                            filePart.getContentType()
                    );
                }

                // Lấy URL ảnh
                String imageUrl =
                        MinioConfig.getObjectUrl(
                                bucketName,
                                objectName
                        );

                // Update avatar
                users.setAvatar(imageUrl);

            } catch (Exception e) {

                throw new RuntimeException(
                        "Upload avatar thất bại",
                        e
                );
            }
        }

        if (req.getRoleId() != null && !req.getRoleId().isEmpty()) {

            Set<Role> roles = new HashSet<>();

            for (Long roleId : req.getRoleId()) {

                Role role = roleDao.findById(roleId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Role không tồn tại: " + roleId
                                )
                        );

                roles.add(role);
            }

            users.setRole(roles);
        }

        // 7. Lưu database
        User updated = usersDao.update(users);

        // 8. Convert Entity -> Response
        return UserMapper.toResponse(updated);
    }

    @Override
    public void exportExcel(OutputStream outputStream, UserType type, UserStatus status, String keyword, int page, int size) {
        List<User> users=usersDao.search(type,status,keyword,page,size);
       try{
           ExcelUtils.exportExcelUser(outputStream,users);
       }catch (Exception e){
           throw new RuntimeException("Lỗi xuất excel",e);
       }
    }

//    @Override
//    public void importExcel(InputStream inputStream) {
//        try{
//            List<UsersResquest> request=ExcelUtils.importExcelUser(inputStream);
//            for(UsersResquest req:request){
//                Role role=roleDao.findById(req.getRoleId())
//                        .orElseThrow(()-> new ResourceNotFoundException("Role không tồn tại " + req.getRoleId()));
//                User user=UserMapper.toEntity(req,role);
//                user.setPassword(PasswordUntil.hashedPassword(req.getPassword()));
//                usersDao.save(user);
//            }
//
//
//        }catch (Exception e){
//            throw new RuntimeException("lỗi importExcel User",e);
//        }
//    }
}

