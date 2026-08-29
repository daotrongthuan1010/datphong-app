package com.vivu.booking.utils;

import com.vivu.booking.dto.request.UsersResquest;
import com.vivu.booking.dto.response.UsersResponse;
import com.vivu.booking.entity.Booking;
import com.vivu.booking.entity.User;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ExcelUtils {
        public static void exportExcelUser(OutputStream outputStream, List<User> users)throws Exception {
            try(Workbook workbook = new XSSFWorkbook() ) {
                Sheet sheet =workbook.createSheet("Users");
                String[] headers={"Họ Và Tên","Email","Phone","Username","Gender","Avatar","Status","Active"};
                CellStyle headerStyle = workbook.createCellStyle();
                headerStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                Row header = sheet.createRow(0);
                for (int i = 0; i < headers.length; i++) {
                    header.createCell(i).setCellValue(headers[i]);
                }
                int rowIndex=1;
                for(User u:users){
                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(u.getFullName());
                    row.createCell(1).setCellValue(u.getEmail());
                    row.createCell(2).setCellValue(u.getPhone());
                    row.createCell(3).setCellValue(u.getUsername());
                    row.createCell(4).setCellValue(String.valueOf(u.getGender()?"Nam":"Nữ"));
                    row.createCell(5).setCellValue(u.getAvatar());
                    row.createCell(6).setCellValue(String.valueOf(u.getStatus()));
                    row.createCell(7).setCellValue(u.getActive());

                }
                workbook.write(outputStream);

            } catch (IOException e) {
                throw new RuntimeException("Lỗi khi xuất Excel", e);

            }
        }

    public static List<UsersResquest> importExcelUser(InputStream inputStream) throws Exception {
        List<UsersResquest> users = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            // Bỏ dòng header
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {continue;}
                UsersResquest request = new UsersResquest();
                request.setFullName(getCellString(row.getCell(0)));
                request.setEmail(getCellString(row.getCell(1)));
                request.setPhone(getCellString(row.getCell(2)));
                request.setUsername(getCellString(row.getCell(3)));
                String gender = getCellString(row.getCell(4));
                request.setGender("Nam".equalsIgnoreCase(gender));
                request.setAvatar(getCellString(row.getCell(5)));
                String status = getCellString(row.getCell(6));
                if (!status.isBlank()) {request.setStatus(
                            com.vivu.booking.enums.UserStatus.valueOf(
                                    status
                            )
                    );
                }
                String active = getCellString(row.getCell(7));
                if (!active.isBlank()) {request.setActive(
                        Boolean.parseBoolean(active)
                    );
                }
                users.add(request);
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Lỗi khi đọc file Excel",
                    e
            );
        }
        return users;
    }


    // =========================
    // ĐỌC CELL
    // =========================
    private static String getCellString(Cell cell) {
        if (cell == null) {return "";}
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }
}
