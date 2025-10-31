package com.mi.project.util;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
/**
 * Excel工具类
 * 提供Excel文件的导入导出功能
 * @author 31591
 */
@Slf4j
@Component
public class ExcelUtil {

    private static final String DEFAULT_SHEET_NAME = "Sheet1";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /**
     * 导出数据到Excel
     */
    public <T> void exportToExcel(List<T> data, String fileName, HttpServletResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(DEFAULT_SHEET_NAME);
            
            if (data == null || data.isEmpty()) {
                log.warn("导出数据为空");
                // 创建空的工作表
                Row emptyRow = sheet.createRow(0);
                emptyRow.createCell(0).setCellValue("暂无数据");
                return;
            }
            // 创建标题行
            T firstItem = data.get(0);
            List<String> headers = getHeaders(firstItem.getClass());
            Row headerRow = sheet.createRow(0);
            // 设置标题样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }
            // 填充数据
            for (int i = 0; i < data.size(); i++) {
                Row row = sheet.createRow(i + 1);
                T item = data.get(i);
                fillRowData(row, item, headers);
            }
            // 自动调整列宽
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
                // 设置最大列宽，避免过宽
                if (sheet.getColumnWidth(i) > 15000) {
                    sheet.setColumnWidth(i, 15000);
                }
            }
            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + 
                    java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8));
            // 写入响应
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
            log.info("Excel导出成功: fileName={}, rows={}", fileName, data.size());
        } catch (IOException e) {
            log.error("Excel导出失败: fileName={}", fileName, e);
            throw new RuntimeException("Excel导出失败", e);
        }
    }
    /**
     * 从Excel导入数据
     */
    public <T> List<T> importFromExcel(MultipartFile file, Class<T> clazz) {
        List<T> result = new ArrayList<>();
        
        // 验证文件
        if (file == null || file.isEmpty()) {
            log.warn("上传文件为空");
            return result;
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
            log.warn("文件格式不支持: {}", fileName);
            throw new RuntimeException("只支持Excel文件格式(.xlsx, .xls)");
        }
        
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                log.warn("Excel文件没有工作表");
                return result;
            }

            // 获取标题行
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                log.warn("Excel文件没有标题行");
                return result;
            }

            List<String> headers = new ArrayList<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                String headerValue = getCellValueAsString(cell);
                if (headerValue != null && !headerValue.trim().isEmpty()) {
                    headers.add(headerValue.trim());
                } else {
                    headers.add("column_" + i); // 默认列名
                }
            }
            // 解析数据行
            int successCount = 0;
            int errorCount = 0;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    T item = parseRowToObject(row, clazz, headers);
                    if (item != null) {
                        result.add(item);
                        successCount++;
                    }
                } catch (Exception e) {
                    log.warn("解析第{}行数据失败: {}", i + 1, e.getMessage());
                    errorCount++;
                }
            }

            log.info("Excel导入完成: fileName={}, 成功={}, 失败={}", fileName, successCount, errorCount);
            return result;

        } catch (IOException e) {
            log.error("Excel导入失败: fileName={}", fileName, e);
            throw new RuntimeException("Excel导入失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建标题样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    /**
     * 获取对象的所有字段名作为标题
     */
    private List<String> getHeaders(Class<?> clazz) {
        List<String> headers = new ArrayList<>();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            // 跳过序列化相关字段
            if (field.getName().equals("serialVersionUID")) continue;
            headers.add(field.getName());
        }

        return headers;
    }

    /**
     * 填充行数据
     */
    private <T> void fillRowData(Row row, T item, List<String> headers) {
        Class<?> clazz = item.getClass();

        for (int i = 0; i < headers.size(); i++) {
            String fieldName = headers.get(i);
            Cell cell = row.createCell(i);

            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(item);

                if (value != null) {
                    setCellValue(cell, value);
                }
            } catch (Exception e) {
                log.warn("设置字段值失败: field={}", fieldName, e);
            }
        }
    }

    /**
     * 设置单元格值
     */
    private void setCellValue(Cell cell, Object value) {
        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof Date) {
            cell.setCellValue((Date) value);
        } else if (value instanceof LocalDateTime) {
            cell.setCellValue(((LocalDateTime) value).format(DATE_FORMATTER));
        } else {
            cell.setCellValue(value.toString());
        }
    }

    /**
     * 解析行数据为对象
     */
    private <T> T parseRowToObject(Row row, Class<T> clazz, List<String> headers) {
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();

            for (int i = 0; i < headers.size() && i < row.getLastCellNum(); i++) {
                String fieldName = headers.get(i);
                Cell cell = row.getCell(i);

                if (cell == null) continue;

                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);

                    Object value = parseCellValue(cell, field.getType());
                    field.set(instance, value);
                } catch (Exception e) {
                    log.warn("解析字段失败: field={}", fieldName, e);
                }
            }

            return instance;
        } catch (Exception e) {
            log.error("创建对象实例失败", e);
            return null;
        }
    }

    /**
     * 解析单元格值
     */
    private Object parseCellValue(Cell cell, Class<?> targetType) {
        String cellValue = getCellValueAsString(cell);

        if (cellValue == null || cellValue.trim().isEmpty()) {
            return null;
        }

        try {
            if (targetType == String.class) {
                return cellValue;
            } else if (targetType == Integer.class || targetType == int.class) {
                return Integer.parseInt(cellValue);
            } else if (targetType == Long.class || targetType == long.class) {
                return Long.parseLong(cellValue);
            } else if (targetType == Double.class || targetType == double.class) {
                return Double.parseDouble(cellValue);
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                return Boolean.parseBoolean(cellValue);
            } else if (targetType == LocalDateTime.class) {
                return LocalDateTime.parse(cellValue, DATE_FORMATTER);
            } else {
                return cellValue;
            }
        } catch (Exception e) {
            log.warn("类型转换失败: value={}, targetType={}", cellValue, targetType);
            return cellValue;
        }
    }

    /**
     * 获取单元格值作为字符串
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
}

