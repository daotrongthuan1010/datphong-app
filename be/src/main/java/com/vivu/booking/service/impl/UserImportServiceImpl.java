package com.vivu.booking.service.impl;

import com.vivu.booking.dao.RoleDao;
import com.vivu.booking.dao.UsersDao;
import com.vivu.booking.dto.request.UserImportRequest;
import com.vivu.booking.entity.Role;
import com.vivu.booking.entity.User;
import com.vivu.booking.enums.UserStatus;
import com.vivu.booking.mapper.UserImportMapper;
import com.vivu.booking.service.UserImportService;
import org.apache.poi.ss.usermodel.*;

import java.io.InputStream;
import java.util.*;

public class UserImportServiceImpl implements UserImportService {

    private final UsersDao usersDao;
    private final RoleDao roleDao;

    public UserImportServiceImpl(
            UsersDao usersDao,
            RoleDao roleDao
    ) {
        this.usersDao = usersDao;
        this.roleDao = roleDao;
    }

    @Override
    public void importExcel(InputStream inputStream) throws Exception {

        List<User> users = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);

                if (row == null) {
                    continue;
                }

                UserImportRequest request = UserImportRequest.builder()
                        .fullName(getString(row.getCell(0)))
                        .email(getString(row.getCell(1)))
                        .phone(getString(row.getCell(2)))
                        .username(getString(row.getCell(3)))
                        .password(getString(row.getCell(4)))
                        .gender(getBoolean(row.getCell(5)))
                        .avatar(getString(row.getCell(6)))
                        .status(getStatus(row.getCell(7)))
                        .active(getBoolean(row.getCell(8)))
                        .roleId(parseRoleIds(getString(row.getCell(9))))
                        .build();

                // Lấy Role từ database
                Set<Role> roles = roleDao.findByIds(
                        request.getRoleId()
                );

                // Mapping Request -> User
                User user = UserImportMapper.toEntity(
                        request,
                        roles
                );

                users.add(user);
            }
        }

        if (!users.isEmpty()) {
            usersDao.saveBatch(users);
        }
    }

    private String getString(Cell cell) {

        if (cell == null) {
            return null;
        }

        DataFormatter formatter = new DataFormatter();

        String value = formatter.formatCellValue(cell).trim();

        return value.isEmpty() ? null : value;
    }

    private Boolean getBoolean(Cell cell) {

        String value = getString(cell);

        if (value == null) {
            return null;
        }

        return Boolean.parseBoolean(value);
    }

    private UserStatus getStatus(Cell cell) {

        String value = getString(cell);

        if (value == null) {
            return null;
        }

        return UserStatus.valueOf(value.toUpperCase());
    }

    private Set<Long> parseRoleIds(String value) {

        if (value == null || value.isBlank()) {
            return new HashSet<>();
        }

        Set<Long> roleIds = new HashSet<>();

        for (String id : value.split(",")) {
            roleIds.add(Long.parseLong(id.trim()));
        }

        return roleIds;
    }
}