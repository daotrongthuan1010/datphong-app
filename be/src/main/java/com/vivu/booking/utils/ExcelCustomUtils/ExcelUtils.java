package com.vivu.booking.utils.ExcelCustomUtils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class ExcelUtils {
    public static byte[] export(String sheetName, String[] headers, List<String[]> data) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {
            Sheet sheet = workbook.createSheet(sheetName);
            SXSSFSheet sxssfSheet = (SXSSFSheet) sheet;
            sxssfSheet.trackAllColumnsForAutoSizing();
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillBackgroundColor(IndexedColors.RED.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            int rowIndex = 1;
            for (String[] values : data) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < values.length; i++) {
                    row.createCell(i).setCellValue(values[i] != null ? values[i] : "");
                }
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(outputStream);
            workbook.dispose();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất Excel", e);
        }
    }
}
